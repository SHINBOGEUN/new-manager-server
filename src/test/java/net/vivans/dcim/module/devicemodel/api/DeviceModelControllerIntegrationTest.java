package net.vivans.dcim.module.devicemodel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import net.vivans.dcim.bootstrap.ManagerServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static net.vivans.dcim.support.AuthTestSupport.bearerToken;
import static net.vivans.dcim.support.AuthTestSupport.loginAndGetAccessToken;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class DeviceModelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createAndGetDeviceModel_returnsProtocols() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-user", "password123");
        Integer deviceTypeId = createModelType(accessToken);
        Integer groupId = createCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer mqttId = createCommonCode(accessToken, groupId, "mqtt", "MQTT", 1);

        mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "description": "동작 감지 센서",
                                  "protocols": [
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(deviceTypeId, mqttId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("LHT65N-PIR"))
                .andExpect(jsonPath("$.data.deviceTypeCode").value("SENSOR"))
                .andExpect(jsonPath("$.data.protocols", hasSize(1)))
                .andExpect(jsonPath("$.data.protocols[0].protocolCode").value("mqtt"));

        mockMvc.perform(get("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].manufacturer").value("Dragino"));
    }

    @Test
    void createDuplicateModel_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-dup-user", "password123");
        Integer deviceTypeId = createModelType(accessToken);
        Integer groupId = createCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer mqttId = createCommonCode(accessToken, groupId, "mqtt", "MQTT", 1);

        String body = """
                {
                  "name": "LHT65N-PIR",
                  "manufacturer": "Dragino",
                  "deviceTypeId": %d,
                  "protocols": [
                    { "protocolTypeId": %d }
                  ]
                }
                """.formatted(deviceTypeId, mqttId);

        mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("device model already exists"));
    }

    @Test
    void createDeviceModel_withEmptyProtocols_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-empty-protocol-user", "password123");
        Integer deviceTypeId = createModelType(accessToken);

        mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "protocols": []
                                }
                                """.formatted(deviceTypeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'protocols'"));
    }

    @Test
    void createDeviceModel_withDuplicateProtocolType_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-duplicate-protocol-user", "password123");
        Integer deviceTypeId = createModelType(accessToken);
        Integer groupId = createCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer mqttId = createCommonCode(accessToken, groupId, "mqtt", "MQTT", 1);

        mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d },
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(deviceTypeId, mqttId, mqttId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("duplicate protocol type in request"));
    }

    @Test
    void createDeviceModel_withNonProtocolTypeCommonCode_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-wrong-group-user", "password123");
        Integer deviceTypeId = createModelType(accessToken);
        Integer groupId = createCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer rackId = createCommonCode(accessToken, groupId, "rack", "Rack", 1);

        mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(deviceTypeId, rackId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("protocolType must belong to PROTOCOL_TYPE group"));
    }

    @Test
    void createDeviceModel_withNonModelTypeCommonCode_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-wrong-type-user", "password123");
        Integer protocolGroupId = createCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer mqttId = createCommonCode(accessToken, protocolGroupId, "mqtt", "MQTT", 1);
        Integer locationGroupId = createCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer rackId = createCommonCode(accessToken, locationGroupId, "rack", "Rack", 1);

        mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(rackId, mqttId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("deviceType must belong to MODEL_TYPE group"));
    }

    @Test
    void createDeviceModel_withNullProtocolTypeId_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-null-protocol-user", "password123");
        Integer deviceTypeId = createModelType(accessToken);

        mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": null }
                                  ]
                                }
                                """.formatted(deviceTypeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'protocols[0].protocolTypeId'"));
    }

    @Test
    void updateDeviceModel_keepsExistingProtocolAndAddsNewOne() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-keep-protocol-user", "password123");
        Integer deviceTypeId = createModelType(accessToken);
        Integer groupId = createCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer mqttId = createCommonCode(accessToken, groupId, "mqtt", "MQTT", 1);
        Integer modbusId = createCommonCode(accessToken, groupId, "modbus", "Modbus", 2);

        String createResponse = mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(deviceTypeId, mqttId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int modelId = objectMapper.readTree(createResponse).path("data").path("id").asInt();

        mockMvc.perform(put("/api/manager/device-models/{id}", modelId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "description": "동작 감지 센서",
                                  "protocols": [
                                    { "protocolTypeId": %d },
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(deviceTypeId, modbusId, mqttId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.protocols", hasSize(2)));
    }

    @Test
    void updateDeviceModel_replacesProtocols() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-update-user", "password123");
        Integer deviceTypeId = createModelType(accessToken);
        Integer groupId = createCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer mqttId = createCommonCode(accessToken, groupId, "mqtt", "MQTT", 1);
        Integer modbusId = createCommonCode(accessToken, groupId, "modbus", "Modbus", 2);

        String createResponse = mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(deviceTypeId, mqttId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int modelId = objectMapper.readTree(createResponse).path("data").path("id").asInt();

        mockMvc.perform(put("/api/manager/device-models/{id}", modelId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "description": "updated",
                                  "protocols": [
                                    { "protocolTypeId": %d },
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(deviceTypeId, modbusId, mqttId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("updated"))
                .andExpect(jsonPath("$.data.protocols", hasSize(2)));
    }

    @Test
    void getDeviceModel_whenNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-not-found-user", "password123");

        mockMvc.perform(get("/api/manager/device-models/{id}", 999999)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DeviceModel not found: 999999"));
    }

    @Test
    void updateDeviceModel_whenNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-update-not-found-user", "password123");
        Integer deviceTypeId = createModelType(accessToken);
        Integer groupId = createCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer mqttId = createCommonCode(accessToken, groupId, "mqtt", "MQTT", 1);

        mockMvc.perform(put("/api/manager/device-models/{id}", 999999)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(deviceTypeId, mqttId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DeviceModel not found: 999999"));
    }

    @Test
    void deleteDeviceModel_removesModel() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-delete-user", "password123");
        Integer deviceTypeId = createModelType(accessToken);
        Integer groupId = createCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer mqttId = createCommonCode(accessToken, groupId, "mqtt", "MQTT", 1);

        String createResponse = mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "LHT65N-PIR",
                                  "manufacturer": "Dragino",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(deviceTypeId, mqttId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int modelId = objectMapper.readTree(createResponse).path("data").path("id").asInt();

        mockMvc.perform(delete("/api/manager/device-models/{id}", modelId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(modelId));

        mockMvc.perform(get("/api/manager/device-models/{id}", modelId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDeviceModel_whenNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-delete-not-found-user", "password123");

        mockMvc.perform(delete("/api/manager/device-models/{id}", 999999)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DeviceModel not found: 999999"));
    }

    @Test
    void deleteDeviceModel_whenReferencedByDevices_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-model-delete-conflict-user", "password123");
        Integer deviceTypeId = createModelType(accessToken);
        Integer groupId = createCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer mqttId = createCommonCode(accessToken, groupId, "mqtt", "MQTT", 1);

        String createResponse = mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "AP8959",
                                  "manufacturer": "APC",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(deviceTypeId, mqttId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int modelId = objectMapper.readTree(createResponse).path("data").path("id").asInt();
        String locationCode = createRootLocation(accessToken, "Rack-Conflict");

        mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "PDU-좌",
                                  "description": "referenced"
                                }
                                """.formatted(modelId, locationCode)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/manager/device-models/{id}", modelId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("device model is referenced by devices"));
    }

    private Integer createModelType(String accessToken) throws Exception {
        Integer groupId = createCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        return createCommonCode(accessToken, groupId, "SENSOR", "Sensor", 1);
    }

    private Integer createCodeGroup(String accessToken, String groupKey, String groupName) throws Exception {
        String response = mockMvc.perform(post("/api/manager/code-groups")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "%s", "groupName": "%s"}
                                """.formatted(groupKey, groupName)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private Integer createCommonCode(
            String accessToken,
            Integer groupId,
            String code,
            String name,
            Integer sortOrder
    ) throws Exception {
        String response = mockMvc.perform(post("/api/manager/common-codes")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": %d, "code": "%s", "name": "%s", "sortOrder": %d}
                                """.formatted(groupId, code, name, sortOrder)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private String createRootLocation(String accessToken, String name) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer rackTypeId = findOrCreateCommonCode(accessToken, groupId, "RACK", "랙", 3);
        return createLocationNode(accessToken, null, rackTypeId, name);
    }

    private Integer findOrCreateCodeGroup(String accessToken, String groupKey, String groupName) throws Exception {
        String listResponse = mockMvc.perform(get("/api/manager/code-groups")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (com.fasterxml.jackson.databind.JsonNode node : objectMapper.readTree(listResponse).path("data")) {
            if (groupKey.equals(node.path("groupKey").asText())) {
                return node.path("id").asInt();
            }
        }

        return createCodeGroup(accessToken, groupKey, groupName);
    }

    private Integer findOrCreateCommonCode(
            String accessToken,
            Integer groupId,
            String code,
            String name,
            Integer sortOrder
    ) throws Exception {
        String listResponse = mockMvc.perform(get("/api/manager/common-codes")
                        .param("codeGroupId", String.valueOf(groupId))
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (com.fasterxml.jackson.databind.JsonNode node : objectMapper.readTree(listResponse).path("data")) {
            if (code.equals(node.path("code").asText())) {
                return node.path("id").asInt();
            }
        }

        return createCommonCode(accessToken, groupId, code, name, sortOrder);
    }

    private String createLocationNode(
            String accessToken,
            String parentCode,
            Integer locationTypeId,
            String name
    ) throws Exception {
        String parentJson = parentCode == null ? "null" : "\"%s\"".formatted(parentCode);
        String response = mockMvc.perform(post("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCode": %s, "locationTypeId": %d, "name": "%s"}
                                """.formatted(parentJson, locationTypeId, name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("code").asText();
    }
}
