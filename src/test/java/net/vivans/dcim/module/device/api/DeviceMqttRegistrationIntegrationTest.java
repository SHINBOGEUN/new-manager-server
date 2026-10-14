package net.vivans.dcim.module.device.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.vivans.dcim.bootstrap.ManagerServerApplication;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CodeGroupRepository;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static net.vivans.dcim.support.AuthTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP 요청별 실제 커밋/롤백을 검증하며, 외부 서비스와 실DB에 연결하지 않는다. */
@SpringBootTest(classes = ManagerServerApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:mqtt_device_registration;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "sensor-data.service.enabled=false", "collector.service.enabled=false", "influx.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DeviceMqttRegistrationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired CodeGroupRepository groups;
    @Autowired CommonCodeRepository codes;
    @Autowired DeviceModelRepository models;
    @Autowired LocationNodeRepository locations;
    @Autowired DeviceRepository devices;
    @Autowired DeviceProtocolEndpointRepository endpoints;
    @Autowired DeviceMqttIdentityRepository identities;
    @Autowired PlatformTransactionManager transactions;
    private String authorization;

    @BeforeEach
    void login() throws Exception {
        authorization = bearerToken(loginAndGetAccessToken(mvc, mapper, users,
                "mqtt-admin-" + UUID.randomUUID(), "password123"));
    }

    @Test
    void existingWizardAndDeviceUpdateSaveMacAndPreserveBothIds() throws Exception {
        Fixture fixture = fixture("GPU_SERVER", "mqtt");
        mvc.perform(get("/api/manager/device-registrations/options").param("modelId", fixture.modelId().toString())
                        .header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.protocols[0].configuration").value("MQTT_IDENTITY"));
        JsonNode registered = register(fixture, "GPU first", "MAC", "9c:6b:00:9b:4b:d7", true);
        int deviceId = registered.path("id").asInt();
        int identityId = registered.path("mqttIdentities").get(0).path("id").asInt();
        assertThat(endpoints.findAllByDeviceIdOrderByIdAsc(deviceId)).isEmpty();

        ObjectNode update = deviceBody(fixture, "GPU first");
        addIdentity(update, "MAC", "AA-BB-CC-DD-EE-01", true);
        mvc.perform(put("/api/manager/devices/{id}", deviceId).header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(deviceId))
                .andExpect(jsonPath("$.data.mqttIdentities[0].id").value(identityId))
                .andExpect(jsonPath("$.data.mqttIdentities[0].normalizedExternalId").value("AABBCCDDEE01"));
        assertThat(identities.findByIdentityTypeAndNormalizedExternalId(MqttIdentityType.MAC, "9C6B009B4BD7")).isEmpty();
        mvc.perform(get("/api/manager/devices/{id}", deviceId).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mqttIdentities[0].id").value(identityId));
        mvc.perform(get("/api/manager/devices").param("modelId", fixture.modelId().toString()).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].mqttIdentities[0].externalId").value("AA-BB-CC-DD-EE-01"));
        JsonNode second = register(fixture, "GPU second", "MAC", "AA:BB:CC:DD:EE:02", true);
        assertThat(second.path("id").asInt()).isNotEqualTo(deviceId);
    }

    @Test
    void conflictingDisabledMacRejectsWholeUpdate() throws Exception {
        Fixture fixture = fixture("GPU_SERVER", "mqtt");
        register(fixture, "Occupied", "MAC", "AA:BB:CC:DD:EE:11", false);
        int deviceId = register(fixture, "Original name", "MAC", "AA:BB:CC:DD:EE:12", true).path("id").asInt();
        ObjectNode update = deviceBody(fixture, "Should rollback");
        addIdentity(update, "MAC", "aabbccddee11", true);
        mvc.perform(put("/api/manager/devices/{id}", deviceId).header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/manager/devices/{id}", deviceId).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Original name"))
                .andExpect(jsonPath("$.data.mqttIdentities[0].externalId").value("AA:BB:CC:DD:EE:12"));
    }

    @Test
    void invalidMacAndMissingIdentityRollbackWizardRegistration() throws Exception {
        Fixture fixture = fixture("GPU_SERVER", "mqtt");
        int before = devices.findAllByDeviceModelId(fixture.modelId()).size();
        ObjectNode device = deviceBody(fixture, "Invalid MAC");
        addIdentity(device, "MAC", "not-a-mac", true);
        ObjectNode body = wizardBody(fixture, device);
        mvc.perform(post("/api/manager/device-registrations").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        body = wizardBody(fixture, deviceBody(fixture, "No identity"));
        mvc.perform(post("/api/manager/device-registrations").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        assertThat(devices.findAllByDeviceModelId(fixture.modelId())).hasSize(before);
    }

    @Test
    void omittedIdentitiesAreRetainedAndEmptyListRemovesOnlyMapping() throws Exception {
        Fixture fixture = fixture("GPU_SERVER", "mqtt");
        int deviceId = register(fixture, "Keep MAC", "MAC", "AA:BB:CC:DD:EE:21", true).path("id").asInt();
        ObjectNode update = deviceBody(fixture, "Rename only");
        mvc.perform(put("/api/manager/devices/{id}", deviceId).header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mqttIdentities", hasSize(1)));
        update.putArray("mqttIdentities");
        mvc.perform(put("/api/manager/devices/{id}", deviceId).header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(deviceId))
                .andExpect(jsonPath("$.data.mqttIdentities", hasSize(0)));
        assertThat(devices.findById(deviceId)).isPresent();
    }

    @Test
    void loraLegacyWizardAndCommonDeviceEditorBothRemainCompatible() throws Exception {
        Fixture fixture = fixture("LORA_SENSOR", "mqtt");
        ObjectNode body = wizardBody(fixture, deviceBody(fixture, "Legacy LoRa"));
        body.putObject("loraIdentity").put("idType", "DEV_EUI").put("externalId", "24E124710C123456");
        String response = mvc.perform(post("/api/manager/device-registrations").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.device.mqttIdentities[0].identityType").value("DEV_EUI"))
                .andReturn().getResponse().getContentAsString();
        int deviceId = mapper.readTree(response).path("data").path("device").path("id").asInt();
        ObjectNode update = deviceBody(fixture, "Legacy LoRa");
        addIdentity(update, "DEV_EUI", "24:E1:24:71:0C:12:34:57", true);
        addIdentity(update, "DEVICE_NAME", "Dragino-legacy", true);
        mvc.perform(put("/api/manager/devices/{id}", deviceId).header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mqttIdentities", hasSize(2)));
        mvc.perform(get("/api/manager/lora/endpoints/resolve").header("Authorization", authorization)
                        .param("idType", "DEV_EUI").param("externalId", "24E124710C123457"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.deviceId").value(deviceId));
        JsonNode modern = register(fixture, "Modern LoRa", "DEV_EUI", "24E124710C123458", true);
        assertThat(modern.path("mqttIdentities").get(0).path("identityType").asText()).isEqualTo("DEV_EUI");
    }

    @Test
    void deletingDeviceAlsoDeletesItsIdentity() throws Exception {
        Fixture fixture = fixture("GPU_SERVER", "mqtt");
        JsonNode device = register(fixture, "Delete GPU", "MAC", "AA:BB:CC:DD:EE:31", true);
        int deviceId = device.path("id").asInt();
        int identityId = device.path("mqttIdentities").get(0).path("id").asInt();
        mvc.perform(delete("/api/manager/devices/{id}", deviceId).header("Authorization", authorization))
                .andExpect(status().isOk());
        assertThat(identities.findById(identityId)).isEmpty();
        assertThat(devices.findById(deviceId)).isEmpty();
    }

    private JsonNode register(Fixture fixture, String name, String type, String value, boolean enabled) throws Exception {
        ObjectNode device = deviceBody(fixture, name);
        addIdentity(device, type, value, enabled);
        String response = mvc.perform(post("/api/manager/device-registrations").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(wizardBody(fixture, device))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).path("data").path("device");
    }

    private ObjectNode deviceBody(Fixture fixture, String name) {
        return mapper.createObjectNode().put("modelId", fixture.modelId()).put("locationNodeCode", "UNASSIGNED").put("name", name);
    }

    private void addIdentity(ObjectNode device, String type, String value, boolean enabled) {
        device.withArray("mqttIdentities").addObject().put("identityType", type).put("externalId", value).put("enabled", enabled);
    }

    private ObjectNode wizardBody(Fixture fixture, ObjectNode device) {
        ObjectNode body = mapper.createObjectNode().put("protocolTypeId", fixture.protocolTypeId());
        body.set("device", device);
        return body;
    }

    private Fixture fixture(String type, String protocol) {
        return new TransactionTemplate(transactions).execute(status -> {
            CommonCode modelType = code("MODEL_TYPE", type);
            CommonCode protocolType = code("PROTOCOL_TYPE", protocol);
            CommonCode locationType = code("LOCATION_TYPE", "UNASSIGNED");
            if (locations.findByCode("UNASSIGNED").isEmpty()) {
                locations.save(LocationNode.createRoot("UNASSIGNED", locationType, "Unassigned"));
            }
            DeviceModel model = DeviceModel.create("MQTT-" + UUID.randomUUID(), "Test", modelType, null);
            model.replaceProtocols(List.of(DeviceModelProtocol.of(model, protocolType)));
            model = models.save(model);
            return new Fixture(model.getId(), protocolType.getId());
        });
    }

    private CommonCode code(String key, String value) {
        return codes.findByCodeGroupGroupKeyAndCode(key, value).orElseGet(() -> {
            CodeGroup group = groups.findAll().stream().filter(item -> key.equals(item.getGroupKey())).findFirst()
                    .orElseGet(() -> groups.save(CodeGroup.createCodeGroup(key, key)));
            return codes.save(CommonCode.createCommonCode(group, value, value, 1));
        });
    }

    private record Fixture(Integer modelId, Integer protocolTypeId) {}
}
