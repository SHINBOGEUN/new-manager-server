package net.vivans.dcim.module.query.api;

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

import java.util.List;

import static net.vivans.dcim.support.AuthTestSupport.bearerToken;
import static net.vivans.dcim.support.AuthTestSupport.loginAndGetAccessToken;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class QueryCountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCount_returnsTotalAndByModel() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "query-count-user", "password123");
        devicePageCodeId(accessToken, "dashboard", "dashboard", 1);
        int modelA = createModel(accessToken, "Count-Model-A", "APC");
        int modelB = createModel(accessToken, "Count-Model-B", "Carrier");
        int a1 = createDevice(accessToken, "Count-A1", modelA, true);
        int a2 = createDevice(accessToken, "Count-A2", modelA, true);
        int b1 = createDevice(accessToken, "Count-B1", modelB, true);
        int widgetId = createCountWidget(accessToken, "dashboard", "장비 수", "by_model", null);

        mockMvc.perform(get("/api/manager/query/count")
                        .param("widgetId", String.valueOf(widgetId))
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.widgetId").value(widgetId))
                .andExpect(jsonPath("$.data.widgetName").value("장비 수"))
                .andExpect(jsonPath("$.data.pageCode").value("dashboard"))
                .andExpect(jsonPath("$.data.count").value(3))
                .andExpect(jsonPath("$.data.byModel", hasSize(2)))
                .andExpect(jsonPath("$.data.byModel[0].modelName").value("Count-Model-A"))
                .andExpect(jsonPath("$.data.byModel[0].manufacturer").value("APC"))
                .andExpect(jsonPath("$.data.byModel[0].count").value(2))
                .andExpect(jsonPath("$.data.byModel[1].modelName").value("Count-Model-B"))
                .andExpect(jsonPath("$.data.byModel[1].count").value(1));
    }

    @Test
    void getCount_excludesDisabledDevices() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "query-count-off", "password123");
        devicePageCodeId(accessToken, "dashboard", "dashboard", 1);
        int modelId = createModel(accessToken, "Count-Model-Off", "APC");
        int on = createDevice(accessToken, "Count-On", modelId, true);
        int off = createDevice(accessToken, "Count-Off", modelId, false);
        int widgetId = createCountWidget(accessToken, "dashboard", "활성만", "by_model", null);

        mockMvc.perform(get("/api/manager/query/count")
                        .param("widgetId", String.valueOf(widgetId))
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.byModel", hasSize(1)))
                .andExpect(jsonPath("$.data.byModel[0].count").value(1));
    }

    @Test
    void getCount_whenWidgetNotCount_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "query-count-kind", "password123");
        devicePageCodeId(accessToken, "dashboard", "dashboard", 1);
        int modelId = createModel(accessToken, "Count-Model-Kind", "APC");
        int deviceId = createDevice(accessToken, "Count-Kind", modelId, true);
        int widgetId = createLastWidget(accessToken, "dashboard", "last위젯", deviceId);

        mockMvc.perform(get("/api/manager/query/count")
                        .param("widgetId", String.valueOf(widgetId))
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("widget queryKind must be count, but was last"));
    }

    @Test
    void getCount_whenWidgetIdMissing_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "query-count-bad", "password123");

        mockMvc.perform(get("/api/manager/query/count")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCount_totalMode_returnsCountOnly() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "query-count-total", "password123");
        devicePageCodeId(accessToken, "dashboard", "dashboard", 1);
        int modelA = createModel(accessToken, "Count-Total-A", "APC");
        int modelB = createModel(accessToken, "Count-Total-B", "Carrier");
        int a1 = createDevice(accessToken, "Count-T-A1", modelA, true);
        int b1 = createDevice(accessToken, "Count-T-B1", modelB, true);
        int widgetId = createCountWidget(accessToken, "dashboard", "전체", "total", null);

        mockMvc.perform(get("/api/manager/query/count")
                        .param("widgetId", String.valueOf(widgetId))
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.countMode").value("total"))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.byModel").isEmpty());
    }

    @Test
    void getCount_modelMode_filtersByModelId() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "query-count-model", "password123");
        devicePageCodeId(accessToken, "dashboard", "dashboard", 1);
        int modelA = createModel(accessToken, "Count-Filter-A", "APC");
        int modelB = createModel(accessToken, "Count-Filter-B", "Carrier");
        int a1 = createDevice(accessToken, "Count-F-A1", modelA, true);
        int a2 = createDevice(accessToken, "Count-F-A2", modelA, true);
        int b1 = createDevice(accessToken, "Count-F-B1", modelB, true);
        int widgetId = createCountWidget(accessToken, "dashboard", "APC만", "model", modelA);

        mockMvc.perform(get("/api/manager/query/count")
                        .param("widgetId", String.valueOf(widgetId))
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.countMode").value("model"))
                .andExpect(jsonPath("$.data.countModelId").value(modelA))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.byModel", hasSize(1)))
                .andExpect(jsonPath("$.data.byModel[0].modelId").value(modelA));
    }

    private int createCountWidget(
            String accessToken,
            String pageCode,
            String name,
            String countMode,
            Integer countModelId
    ) throws Exception {
        String modelPart = countModelId == null ? "" : ", \"countModelId\": " + countModelId;
        String response = mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode": "%s",
                                  "name": "%s",
                                  "queryKind": "count",
                                  "countMode": "%s"%s,
                                  "deviceIds": []
                                }
                                """.formatted(pageCode, name, countMode, modelPart)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private int createLastWidget(String accessToken, String pageCode, String name, int deviceId) throws Exception {
        String response = mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode": "%s",
                                  "name": "%s",
                                  "queryKind": "last",
                                  "deviceIds": [%d],
                                  "pointNames": ["V"]
                                }
                                """.formatted(pageCode, name, deviceId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private int createModel(String accessToken, String modelName, String manufacturer) throws Exception {
        Integer protocolGroupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer snmpId = findOrCreateCommonCode(accessToken, protocolGroupId, "snmp", "SNMP", 1);
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
                                  "protocols": [ { "protocolTypeId": %d } ]
                                }
                                """.formatted(modelName, manufacturer, deviceTypeId, snmpId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode modelNode = objectMapper.readTree(modelResponse).path("data");
        int modelId = modelNode.path("id").asInt();
        int protocolId = modelNode.path("protocols").get(0).path("id").asInt();

        mockMvc.perform(post("/api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points",
                        modelId, protocolId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "V",
                                  "oid": "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.%d.0",
                                  "requiresInstance": false,
                                  "unit": "V",
                                  "enabled": true
                                }
                                """.formatted(Math.abs(modelName.hashCode() % 900) + 100)))
                .andExpect(status().isOk());
        return modelId;
    }

    private int createDevice(String accessToken, String name, int modelId, boolean enabled) throws Exception {
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
                                  "name": "%s",
                                  "enabled": %s
                                }
                                """.formatted(modelId, locationCode, name, enabled)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(deviceResponse).path("data").path("id").asInt();
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
