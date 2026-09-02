package net.vivans.dcim.module.query.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.bootstrap.ManagerServerApplication;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static net.vivans.dcim.support.AuthTestSupport.bearerToken;
import static net.vivans.dcim.support.AuthTestSupport.loginAndGetAccessToken;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class QueryLastControllerIntegrationTest {

    private static final Instant TIME = Instant.parse("2026-08-21T13:51:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PointQuery pointQuery;

    @Test
    void getLast_returnsLatestValuesForWidgetDevices() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "query-last-user", "password123");
        devicePageCodeId(accessToken, "dashboard", "dashboard", 1);
        CreatedDevice deviceA = createDeviceWithPoints(accessToken, "Q-Last-A", Map.of("V", "V", "temp", "C"));
        CreatedDevice deviceB = createDeviceWithPoints(accessToken, "Q-Last-B", Map.of("V", "V", "temp", "C"));
        int widgetId = createLastWidget(
                accessToken, "dashboard", "칠러", List.of(deviceA.deviceId(), deviceB.deviceId()), List.of("V", "temp"));

        when(pointQuery.findLast(eq(List.of(deviceA.deviceId(), deviceB.deviceId())), eq(List.of("V", "temp")), any()))
                .thenReturn(List.of(
                        new LastPoint(deviceA.deviceId(), "V", 219.0, TIME),
                        new LastPoint(deviceB.deviceId(), "temp", 24.1, TIME)
                ));

        mockMvc.perform(get("/api/manager/query/last")
                        .param("widgetId", String.valueOf(widgetId))
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.widgetId").value(widgetId))
                .andExpect(jsonPath("$.data.widgetName").value("칠러"))
                .andExpect(jsonPath("$.data.pageCode").value("dashboard"))
                .andExpect(jsonPath("$.data.devices", hasSize(2)))
                .andExpect(jsonPath("$.data.devices[0].deviceId").value(deviceA.deviceId()))
                .andExpect(jsonPath("$.data.devices[0].deviceName").value("Q-Last-A"))
                .andExpect(jsonPath("$.data.devices[0].points[0].pointName").value("V"))
                .andExpect(jsonPath("$.data.devices[0].points[0].unit").value("V"))
                .andExpect(jsonPath("$.data.devices[0].points[0].value").value(219.0))
                .andExpect(jsonPath("$.data.devices[1].deviceId").value(deviceB.deviceId()))
                .andExpect(jsonPath("$.data.devices[1].points[0].pointName").value("temp"))
                .andExpect(jsonPath("$.data.devices[1].points[0].unit").value("C"));
    }

    @Test
    void getLast_whenWidgetIdMissing_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "query-last-bad", "password123");

        mockMvc.perform(get("/api/manager/query/last")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLast_whenWidgetUnknown_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "query-last-404", "password123");

        mockMvc.perform(get("/api/manager/query/last")
                        .param("widgetId", "999999")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PageWidget not found: 999999"));
    }

    @Test
    void getLast_whenWidgetNotLast_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "query-last-kind", "password123");
        devicePageCodeId(accessToken, "dashboard", "dashboard", 1);
        int deviceId = createDevice(accessToken, "Q-Agg");
        int widgetId = createAggregateWidget(accessToken, "dashboard", "일일전력", deviceId);

        mockMvc.perform(get("/api/manager/query/last")
                        .param("widgetId", String.valueOf(widgetId))
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("widget queryKind must be last, but was aggregate"));
    }

    private int createLastWidget(
            String accessToken,
            String pageCode,
            String name,
            List<Integer> deviceIds,
            List<String> pointNames
    ) throws Exception {
        String deviceJson = deviceIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        String pointJson = pointNames.stream()
                .map(point -> "\"" + point + "\"")
                .reduce((a, b) -> a + "," + b)
                .map(inner -> "[" + inner + "]")
                .orElse("[]");
        String response = mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode": "%s",
                                  "name": "%s",
                                  "queryKind": "last",
                                  "deviceIds": [%s],
                                  "pointNames": %s
                                }
                                """.formatted(pageCode, name, deviceJson, pointJson)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private int createAggregateWidget(
            String accessToken,
            String pageCode,
            String name,
            int deviceId
    ) throws Exception {
        String response = mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode": "%s",
                                  "name": "%s",
                                  "queryKind": "aggregate",
                                  "op": "usage",
                                  "aggregateRangePreset": "today",
                                  "deviceIds": [%d],
                                  "pointNames": []
                                }
                                """.formatted(pageCode, name, deviceId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private int createDevice(String accessToken, String name) throws Exception {
        return createDeviceWithPoints(accessToken, name, Map.of()).deviceId();
    }

    private CreatedDevice createDeviceWithPoints(
            String accessToken,
            String name,
            Map<String, String> pointUnits
    ) throws Exception {
        Integer protocolGroupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer snmpId = findOrCreateCommonCode(accessToken, protocolGroupId, "snmp", "SNMP", 1);
        Integer modelTypeGroupId = findOrCreateCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer deviceTypeId = findOrCreateCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);

        String modelResponse = mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Model-%s",
                                  "manufacturer": "APC",
                                  "deviceTypeId": %d,
                                  "protocols": [ { "protocolTypeId": %d } ]
                                }
                                """.formatted(name, deviceTypeId, snmpId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode modelNode = objectMapper.readTree(modelResponse).path("data");
        int modelId = modelNode.path("id").asInt();
        int protocolId = modelNode.path("protocols").get(0).path("id").asInt();

        int oidSuffix = 1;
        for (Map.Entry<String, String> entry : pointUnits.entrySet()) {
            mockMvc.perform(post("/api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points",
                            modelId, protocolId)
                            .header("Authorization", bearerToken(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "%s",
                                      "oid": "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.%d.0",
                                      "requiresInstance": false,
                                      "unit": "%s",
                                      "enabled": true
                                    }
                                    """.formatted(entry.getKey(), oidSuffix++, entry.getValue())))
                    .andExpect(status().isOk());
        }

        Integer locationGroupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer rackTypeId = findOrCreateCommonCode(accessToken, locationGroupId, "RACK", "랙", 3);
        String locationResponse = mockMvc.perform(post("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCode": null, "locationTypeId": %d, "name": "Rack-%s"}
                                """.formatted(rackTypeId, name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String locationCode = objectMapper.readTree(locationResponse).path("data").path("code").asText();

        String deviceResponse = mockMvc.perform(post("/api/manager/devices")
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

        int deviceId = objectMapper.readTree(deviceResponse).path("data").path("id").asInt();
        return new CreatedDevice(deviceId, modelId);
    }

    private record CreatedDevice(int deviceId, int modelId) {
    }

    private Integer devicePageCodeId(String accessToken, String code, String name, int sortOrder) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "DEVICE_PAGE", "Device Page");
        return findOrCreateCommonCode(accessToken, groupId, code, name, sortOrder);
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
