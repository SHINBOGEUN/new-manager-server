package net.vivans.dcim.module.device.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class DeviceCapabilityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCapabilities_filtersByPageCodeAndLocation() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "cap-filter-user", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer envPageId = devicePageCodeId(accessToken, "ENVIRONMENT", "Environment", 1);
        Integer powerPageId = devicePageCodeId(accessToken, "POWER", "Power", 4);

        Integer pduModelId = createDeviceModelWithSnmpPoints(
                accessToken,
                "CAP-AP8959",
                "APC",
                snmpId,
                true,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.{instanceId}.3",
                "V",
                "V"
        );
        Integer sensorModelId = createDeviceModelWithSnmpPoints(
                accessToken,
                "CAP-LHT65N",
                "Dragino",
                snmpId,
                false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.1.0",
                "temp",
                "C"
        );

        String zoneCode = createRootLocation(accessToken, "Cap-Zone");
        String rackCode = createChildLocation(accessToken, zoneCode, "Cap-Rack");
        int pduId = createDevice(accessToken, pduModelId, rackCode, "PDU-cap");
        int sensorId = createDevice(accessToken, sensorModelId, rackCode, "Sensor-cap");
        linkPage(accessToken, pduId, envPageId);
        linkPage(accessToken, pduId, powerPageId);
        linkPage(accessToken, sensorId, envPageId);

        int pduEndpointId = createEndpoint(accessToken, pduId, snmpId, "192.168.1.10", 161);
        createEndpoint(accessToken, sensorId, snmpId, "192.168.1.20", 161);
        createSnmpInstance(accessToken, pduId, pduEndpointId, 1);

        mockMvc.perform(get("/api/manager/devices/capabilities")
                        .param("pageCode", "ENVIRONMENT")
                        .param("locationNodeCode", zoneCode)
                        .param("includeSubtree", "true")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].deviceName").value("PDU-cap"))
                .andExpect(jsonPath("$.data[0].endpoint.host").value("192.168.1.10"))
                .andExpect(jsonPath("$.data[0].endpoint.instanceId").value(1))
                .andExpect(jsonPath("$.data[0].points[0].name").value("V"))
                .andExpect(jsonPath("$.data[0].points[0].resolvedOid")
                        .value("1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.1.3"))
                .andExpect(jsonPath("$.data[1].deviceName").value("Sensor-cap"))
                .andExpect(jsonPath("$.data[1].points[0].name").value("temp"))
                .andExpect(jsonPath("$.data[1].points[0].resolvedOid")
                        .value("1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.1.0"));

        mockMvc.perform(get("/api/manager/devices/capabilities")
                        .param("pageCode", "POWER")
                        .param("locationNodeCode", rackCode)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].deviceName").value("PDU-cap"));
    }

    @Test
    void getCapabilities_whenInstanceMissing_returnsNullResolvedOid() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "cap-no-instance", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer envPageId = devicePageCodeId(accessToken, "ENVIRONMENT", "Environment", 1);

        Integer modelId = createDeviceModelWithSnmpPoints(
                accessToken,
                "CAP-NO-INST",
                "APC",
                snmpId,
                true,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.{instanceId}.3",
                "V",
                "V"
        );
        String locationCode = createRootLocation(accessToken, "Cap-NoInst");
        int deviceId = createDevice(accessToken, modelId, locationCode, "PDU-no-inst");
        linkPage(accessToken, deviceId, envPageId);
        createEndpoint(accessToken, deviceId, snmpId, "192.168.1.10", 161);

        mockMvc.perform(get("/api/manager/devices/capabilities")
                        .param("pageCode", "ENVIRONMENT")
                        .param("locationNodeCode", locationCode)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].points[0].requiresInstance").value(true))
                .andExpect(jsonPath("$.data[0].points[0].resolvedOid").value(nullValue()));
    }

    @Test
    void getCapabilities_whenLocationNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "cap-loc-nf", "password123");

        mockMvc.perform(get("/api/manager/devices/capabilities")
                        .param("locationNodeCode", "UNKNOWN01")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("LocationNode not found: UNKNOWN01"));
    }

    @Test
    void getCapabilities_whenNoMatch_returnsEmptyList() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "cap-empty", "password123");
        String locationCode = createRootLocation(accessToken, "Cap-Empty");

        mockMvc.perform(get("/api/manager/devices/capabilities")
                        .param("pageCode", "COOLING")
                        .param("locationNodeCode", locationCode)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    private Integer snmpProtocolTypeId(String accessToken) throws Exception {
        return findOrCreateCommonCode(
                accessToken,
                findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type"),
                "snmp",
                "SNMP",
                1
        );
    }

    private Integer devicePageCodeId(String accessToken, String code, String name, int sortOrder) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "DEVICE_PAGE", "Device Page");
        return findOrCreateCommonCode(accessToken, groupId, code, name, sortOrder);
    }

    private Integer createDeviceModelWithSnmpPoints(
            String accessToken,
            String modelName,
            String manufacturer,
            Integer snmpId,
            boolean requiresInstance,
            String oid,
            String pointName,
            String unit
    ) throws Exception {
        Integer modelTypeGroupId = findOrCreateCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer deviceTypeId = findOrCreateCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);

        String modelResponse = mockMvc.perform(post("/api/manager/device-models")
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
                                """.formatted(modelName, manufacturer, deviceTypeId, snmpId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode modelNode = objectMapper.readTree(modelResponse).path("data");
        int modelId = modelNode.path("id").asInt();
        int protocolId = modelNode.path("protocols").get(0).path("id").asInt();

        mockMvc.perform(post("/api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points", modelId, protocolId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "oid": "%s",
                                  "requiresInstance": %s,
                                  "unit": "%s",
                                  "enabled": true
                                }
                                """.formatted(pointName, oid, requiresInstance, unit)))
                .andExpect(status().isOk());

        return modelId;
    }

    private int createDevice(String accessToken, Integer modelId, String locationCode, String name) throws Exception {
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

    private void linkPage(String accessToken, int deviceId, Integer pageCodeId) throws Exception {
        mockMvc.perform(post("/api/manager/devices/{deviceId}/pages", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCodeId": %d}
                                """.formatted(pageCodeId)))
                .andExpect(status().isOk());
    }

    private int createEndpoint(String accessToken, int deviceId, Integer protocolTypeId, String host, int port)
            throws Exception {
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

    private void createSnmpInstance(String accessToken, int deviceId, int endpointId, int instanceId) throws Exception {
        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance", deviceId, endpointId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId": %d}
                                """.formatted(instanceId)))
                .andExpect(status().isOk());
    }

    private String createRootLocation(String accessToken, String name) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer zoneTypeId = findOrCreateCommonCode(accessToken, groupId, "ZONE", "Zone", 1);
        return createLocationNode(accessToken, null, zoneTypeId, name);
    }

    private String createChildLocation(String accessToken, String parentCode, String name) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer rackTypeId = findOrCreateCommonCode(accessToken, groupId, "RACK", "Rack", 3);
        return createLocationNode(accessToken, parentCode, rackTypeId, name);
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
