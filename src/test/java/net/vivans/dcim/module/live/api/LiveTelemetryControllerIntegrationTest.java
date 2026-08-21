package net.vivans.dcim.module.live.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.bootstrap.ManagerServerApplication;
import net.vivans.dcim.module.live.application.LiveTelemetrySelectionService;
import org.junit.jupiter.api.AfterEach;
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
import static org.hamcrest.Matchers.nullValue;
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
class LiveTelemetryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LiveTelemetrySelectionService liveTelemetrySelectionService;

    @AfterEach
    void clearLiveSelection() {
        liveTelemetrySelectionService.clearSelection();
    }

    @Test
    void getDevices_returnsSnmpCollectablePointsOnly() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "live-dev-user", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Fixture fixture = createPduWithPoints(accessToken, snmpId, "LIVE-DEV", "V", "W");

        mockMvc.perform(get("/api/manager/live/devices")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].deviceId").value(fixture.deviceId()))
                .andExpect(jsonPath("$.data[0].deviceName").value("LIVE-PDU"))
                .andExpect(jsonPath("$.data[0].points[0].name").value("V"))
                .andExpect(jsonPath("$.data[0].points[0].unit").value("V"))
                .andExpect(jsonPath("$.data[0].points[1].name").value("W"));
    }

    @Test
    void getDevices_excludesDeviceWithoutEndpoint() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "live-no-ep", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModelWithSnmpPoints(
                accessToken, "LIVE-NOEP", snmpId, "W", "W", "1.3.6.1.4.1.6375.1.8.0", false);
        String locationCode = createRootLocation(accessToken, "Live-NoEp");
        createDevice(accessToken, modelId, locationCode, "PDU-no-ep");

        mockMvc.perform(get("/api/manager/live/devices")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void putAndGetSelection_storesDevicePointsAndExpiresAt() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "live-sel-user", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Fixture fixture = createPduWithPoints(accessToken, snmpId, "LIVE-SEL", "V", "W");

        mockMvc.perform(put("/api/manager/live/selection")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    { "deviceId": %d, "pointNames": ["W", "V", "W"] }
                                  ]
                                }
                                """.formatted(fixture.deviceId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].deviceId").value(fixture.deviceId()))
                .andExpect(jsonPath("$.data.items[0].pointNames", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].pointNames[0]").value("W"))
                .andExpect(jsonPath("$.data.items[0].pointNames[1]").value("V"))
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());

        mockMvc.perform(get("/api/manager/live/selection")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].pointNames[0]").value("W"))
                .andExpect(jsonPath("$.data.items[0].pointNames[1]").value("V"));
    }

    @Test
    void putSelection_emptyItems_stopsSession() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "live-empty", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Fixture fixture = createPduWithPoints(accessToken, snmpId, "LIVE-EMPTY", "W", "AMP");

        mockMvc.perform(put("/api/manager/live/selection")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [ { "deviceId": %d, "pointNames": ["W"] } ] }
                                """.formatted(fixture.deviceId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)));

        mockMvc.perform(put("/api/manager/live/selection")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [] }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.expiresAt").value(nullValue()));
    }

    @Test
    void deleteSelection_clearsCurrentSelection() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "live-del", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Fixture fixture = createPduWithPoints(accessToken, snmpId, "LIVE-DEL", "W", "V");

        mockMvc.perform(put("/api/manager/live/selection")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [ { "deviceId": %d, "pointNames": ["W"] } ] }
                                """.formatted(fixture.deviceId())))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/manager/live/selection")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.expiresAt").value(nullValue()));
    }

    @Test
    void putSelection_unknownPoint_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "live-bad-pt", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Fixture fixture = createPduWithPoints(accessToken, snmpId, "LIVE-BADPT", "W", "V");

        mockMvc.perform(put("/api/manager/live/selection")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [ { "deviceId": %d, "pointNames": ["AMP"] } ] }
                                """.formatted(fixture.deviceId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "unknown pointName 'AMP' for device " + fixture.deviceId()));
    }

    @Test
    void putSelection_unknownDevice_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "live-bad-dev", "password123");

        mockMvc.perform(put("/api/manager/live/selection")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [ { \"deviceId\": 999999, \"pointNames\": [\"W\"] } ] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("device is not selectable for live SNMP: 999999"));
    }

    private Fixture createPduWithPoints(
            String accessToken,
            Integer snmpId,
            String modelName,
            String pointA,
            String pointB
    ) throws Exception {
        Integer modelId = createDeviceModelWithSnmpPoints(
                accessToken, modelName, snmpId, pointA, "V", "1.3.6.1.4.1.6375.1.1.0", false);
        Integer protocolId = firstProtocolId(accessToken, modelId);
        createSnmpPoint(accessToken, modelId, protocolId, pointB, "W", "1.3.6.1.4.1.6375.1.8.0", false);
        String locationCode = createRootLocation(accessToken, "Live-" + modelName);
        int deviceId = createDevice(accessToken, modelId, locationCode, "LIVE-PDU");
        createEndpoint(accessToken, deviceId, snmpId, "192.168.1.10", 161);
        return new Fixture(deviceId);
    }

    private record Fixture(int deviceId) {
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

    private Integer createDeviceModelWithSnmpPoints(
            String accessToken,
            String modelName,
            Integer snmpId,
            String pointName,
            String unit,
            String oid,
            boolean requiresInstance
    ) throws Exception {
        Integer modelTypeGroupId = findOrCreateCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer deviceTypeId = findOrCreateCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);

        String modelResponse = mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "manufacturer": "OEM",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(modelName, deviceTypeId, snmpId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode modelNode = objectMapper.readTree(modelResponse).path("data");
        int modelId = modelNode.path("id").asInt();
        int protocolId = modelNode.path("protocols").get(0).path("id").asInt();
        createSnmpPoint(accessToken, modelId, protocolId, pointName, unit, oid, requiresInstance);
        return modelId;
    }

    private Integer firstProtocolId(String accessToken, Integer modelId) throws Exception {
        String response = mockMvc.perform(get("/api/manager/device-models/{id}", modelId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("protocols").get(0).path("id").asInt();
    }

    private void createSnmpPoint(
            String accessToken,
            int modelId,
            int protocolId,
            String pointName,
            String unit,
            String oid,
            boolean requiresInstance
    ) throws Exception {
        mockMvc.perform(post("/api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points",
                        modelId, protocolId)
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

    private void createEndpoint(String accessToken, int deviceId, Integer protocolTypeId, String host, int port)
            throws Exception {
        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "%s",
                                  "port": %d
                                }
                                """.formatted(protocolTypeId, host, port)))
                .andExpect(status().isOk());
    }

    private String createRootLocation(String accessToken, String name) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer zoneTypeId = findOrCreateCommonCode(accessToken, groupId, "ZONE", "Zone", 1);
        String parentJson = "null";
        String response = mockMvc.perform(post("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCode": %s, "locationTypeId": %d, "name": "%s"}
                                """.formatted(parentJson, zoneTypeId, name)))
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
