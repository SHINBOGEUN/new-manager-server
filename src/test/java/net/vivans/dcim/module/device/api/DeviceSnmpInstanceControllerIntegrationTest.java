package net.vivans.dcim.module.device.api;

import com.fasterxml.jackson.databind.JsonNode;
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
class DeviceSnmpInstanceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createSnmpInstance_returnsCreated() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-create", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-INST", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-inst");
        int endpointId = createEndpoint(accessToken, deviceId, snmpTypeId, "192.168.1.10", 161);

        mockMvc.perform(post(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endpointId").value(endpointId))
                .andExpect(jsonPath("$.data.deviceId").value(deviceId))
                .andExpect(jsonPath("$.data.instanceId").value(1));
    }

    @Test
    void createSnmpInstance_whenAlreadyExists_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-dup", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-DUP-INST", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-dup-inst");
        int endpointId = createEndpoint(accessToken, deviceId, snmpTypeId, "192.168.1.10", 161);

        String body = """
                {"instanceId": 1}
                """;
        mockMvc.perform(post(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("snmp instance already exists for this endpoint"));
    }

    @Test
    void createSnmpInstance_whenEndpointNotSnmp_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-modbus", "password123");
        Integer protocolGroupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer snmpTypeId = findOrCreateCommonCode(accessToken, protocolGroupId, "snmp", "SNMP", 1);
        Integer modbusTypeId = findOrCreateCommonCode(accessToken, protocolGroupId, "modbus", "Modbus", 2);
        Integer modelId = createDeviceModelWithProtocols(
                accessToken, "AP8959-BOTH", "APC", snmpTypeId, modbusTypeId);
        int snmpProtocolId = snmpProtocolIdOfModel(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, snmpProtocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-modbus-ep");
        int modbusEndpointId = createEndpoint(accessToken, deviceId, modbusTypeId, "192.168.1.10", 502);

        mockMvc.perform(post(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        modbusEndpointId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("endpoint protocol must be snmp"));
    }

    @Test
    void createSnmpInstance_whenNoRequiresInstancePoint_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-no-req", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-FIXED", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "Name", "1.3.6.1.4.1.318.1.1.0", false);
        int deviceId = createDevice(accessToken, modelId, "PDU-fixed");
        int endpointId = createEndpoint(accessToken, deviceId, snmpTypeId, "192.168.1.10", 161);

        mockMvc.perform(post(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("device model has no snmp point requiring instance"));
    }

    @Test
    void createSnmpInstance_whenEndpointNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-ep-nf", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-EP-NF", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-ep-nf");

        mockMvc.perform(post(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        999999
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId": 1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DeviceProtocolEndpoint not found: 999999"));
    }

    @Test
    void createSnmpInstance_withInvalidInstanceId_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-bad-id", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-BAD-ID", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-bad-id");
        int endpointId = createEndpoint(accessToken, deviceId, snmpTypeId, "192.168.1.10", 161);

        mockMvc.perform(post(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'instanceId'"));
    }

    @Test
    void getSnmpInstance_returnsInstance() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-get", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-GET-INST", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-get-inst");
        int endpointId = createEndpoint(accessToken, deviceId, snmpTypeId, "192.168.1.10", 161);

        mockMvc.perform(post(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId": 2}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endpointId").value(endpointId))
                .andExpect(jsonPath("$.data.deviceId").value(deviceId))
                .andExpect(jsonPath("$.data.instanceId").value(2));
    }

    @Test
    void getSnmpInstance_whenNotRegistered_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-get-nf", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-GET-NF", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-get-nf");
        int endpointId = createEndpoint(accessToken, deviceId, snmpTypeId, "192.168.1.10", 161);

        mockMvc.perform(get(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("DeviceSnmpInstance not found for endpoint: " + endpointId));
    }

    @Test
    void getSnmpInstance_whenEndpointNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-get-ep-nf", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-GET-EP-NF", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-get-ep-nf");

        mockMvc.perform(get(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        999999
                )
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DeviceProtocolEndpoint not found: 999999"));
    }

    @Test
    void updateSnmpInstance_returnsUpdated() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-put", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-PUT-INST", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-put-inst");
        int endpointId = createEndpoint(accessToken, deviceId, snmpTypeId, "192.168.1.10", 161);

        mockMvc.perform(post(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId": 1}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId": 11}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endpointId").value(endpointId))
                .andExpect(jsonPath("$.data.deviceId").value(deviceId))
                .andExpect(jsonPath("$.data.instanceId").value(11));
    }

    @Test
    void updateSnmpInstance_whenNotRegistered_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-put-nf", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-PUT-NF", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-put-nf");
        int endpointId = createEndpoint(accessToken, deviceId, snmpTypeId, "192.168.1.10", 161);

        mockMvc.perform(put(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId": 2}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("DeviceSnmpInstance not found for endpoint: " + endpointId));
    }

    @Test
    void deleteSnmpInstance_removesInstance() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-del", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-DEL-INST", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-del-inst");
        int endpointId = createEndpoint(accessToken, deviceId, snmpTypeId, "192.168.1.10", 161);

        mockMvc.perform(post(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId": 1}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(endpointId));

        mockMvc.perform(get(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSnmpInstance_whenNotRegistered_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "snmp-instance-del-nf", "password123");
        Integer snmpTypeId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-DEL-NF", "APC", snmpTypeId);
        int protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, "V", "1.3.6.1.4.1.318.{instanceId}.3", true);
        int deviceId = createDevice(accessToken, modelId, "PDU-del-nf");
        int endpointId = createEndpoint(accessToken, deviceId, snmpTypeId, "192.168.1.10", 161);

        mockMvc.perform(delete(
                        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance",
                        deviceId,
                        endpointId
                )
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("DeviceSnmpInstance not found for endpoint: " + endpointId));
    }

    private Integer snmpProtocolTypeId(String accessToken) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        return findOrCreateCommonCode(accessToken, groupId, "snmp", "SNMP", 1);
    }

    private int createEndpoint(
            String accessToken,
            int deviceId,
            Integer protocolTypeId,
            String host,
            int port
    ) throws Exception {
        String response = mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "%s",
                                  "port": %d
                                }
                                """.formatted(protocolTypeId, host, port)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private void createSnmpPoint(
            String accessToken,
            Integer modelId,
            int protocolId,
            String name,
            String oid,
            boolean requiresInstance
    ) throws Exception {
        mockMvc.perform(post(
                        "/api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points",
                        modelId,
                        protocolId
                )
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "oid": "%s",
                                  "requiresInstance": %s,
                                  "enabled": true
                                }
                                """.formatted(name, oid, requiresInstance)))
                .andExpect(status().isOk());
    }

    private int firstProtocolId(String accessToken, Integer modelId) throws Exception {
        String response = mockMvc.perform(get("/api/manager/device-models/{id}", modelId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("protocols").get(0).path("id").asInt();
    }

    private int snmpProtocolIdOfModel(String accessToken, Integer modelId) throws Exception {
        String response = mockMvc.perform(get("/api/manager/device-models/{id}", modelId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (JsonNode protocol : objectMapper.readTree(response).path("data").path("protocols")) {
            if ("snmp".equals(protocol.path("protocolCode").asText())) {
                return protocol.path("id").asInt();
            }
        }
        throw new IllegalStateException("snmp protocol not found for model: " + modelId);
    }

    private int createDevice(String accessToken, Integer modelId, String name) throws Exception {
        String locationCode = createRootLocation(accessToken, "Rack-" + name);

        String response = mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "%s"
                                }
                                """.formatted(modelId, locationCode, name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private Integer createDeviceModel(
            String accessToken,
            String name,
            String manufacturer,
            Integer protocolTypeId
    ) throws Exception {
        Integer modelTypeGroupId = findOrCreateCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer deviceTypeId = findOrCreateCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);

        String response = mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "manufacturer": "%s",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(name, manufacturer, deviceTypeId, protocolTypeId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private Integer createDeviceModelWithProtocols(
            String accessToken,
            String name,
            String manufacturer,
            Integer protocolTypeId1,
            Integer protocolTypeId2
    ) throws Exception {
        Integer modelTypeGroupId = findOrCreateCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer deviceTypeId = findOrCreateCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);

        String response = mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "manufacturer": "%s",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d },
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(name, manufacturer, deviceTypeId, protocolTypeId1, protocolTypeId2)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private String createRootLocation(String accessToken, String name) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer rackTypeId = findOrCreateCommonCode(accessToken, groupId, "RACK", "랙", 3);
        String response = mockMvc.perform(post("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCode": null, "locationTypeId": %d, "name": "%s"}
                                """.formatted(rackTypeId, name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("code").asText();
    }

    private Integer findOrCreateCodeGroup(String accessToken, String groupKey, String groupName) throws Exception {
        String listResponse = mockMvc.perform(get("/api/manager/code-groups")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (JsonNode node : objectMapper.readTree(listResponse).path("data")) {
            if (groupKey.equals(node.path("groupKey").asText())) {
                return node.path("id").asInt();
            }
        }

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

        for (JsonNode node : objectMapper.readTree(listResponse).path("data")) {
            if (code.equals(node.path("code").asText())) {
                return node.path("id").asInt();
            }
        }

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
}
