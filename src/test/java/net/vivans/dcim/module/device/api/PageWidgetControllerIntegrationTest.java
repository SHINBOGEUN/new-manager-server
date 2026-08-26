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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class PageWidgetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndListWidgets_filtersByPageCode() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "widget-create", "password123");
        devicePageCodeId(accessToken, "COOLING", "Cooling", 1);
        devicePageCodeId(accessToken, "POWER", "Power", 2);
        int coolingDevice = createDevice(accessToken, "Widget-Cool-A");
        int powerDevice = createDevice(accessToken, "Widget-Power-A");

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode": "COOLING",
                                  "name": "칠러",
                                  "queryKind": "last",
                                  "deviceIds": [%d],
                                  "pointNames": ["status", "W"]
                                }
                                """.formatted(coolingDevice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageCode").value("COOLING"))
                .andExpect(jsonPath("$.data.name").value("칠러"))
                .andExpect(jsonPath("$.data.queryKind").value("last"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.deviceIds[0]").value(coolingDevice))
                .andExpect(jsonPath("$.data.pointNames[0]").value("status"));

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode": "POWER",
                                  "name": "전체 전력",
                                  "queryKind": "last",
                                  "deviceIds": [%d],
                                  "pointNames": ["W"]
                                }
                                """.formatted(powerDevice)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manager/widgets")
                        .param("pageCode", "COOLING")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("칠러"));

        mockMvc.perform(get("/api/manager/widgets")
                        .param("pageCode", "POWER")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("전체 전력"));
    }

    @Test
    void createWidget_whenNameDuplicatedOnSamePage_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "widget-dup", "password123");
        devicePageCodeId(accessToken, "COOLING", "Cooling", 1);
        int deviceId = createDevice(accessToken, "Widget-Dup");

        String body = """
                {
                  "pageCode": "COOLING",
                  "name": "칠러",
                  "queryKind": "last",
                  "deviceIds": [%d],
                  "pointNames": ["W"]
                }
                """.formatted(deviceId);
        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("widget name already exists on this page"));
    }

    @Test
    void createWidget_sameNameOnDifferentPages_succeeds() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "widget-same-name", "password123");
        devicePageCodeId(accessToken, "COOLING", "Cooling", 1);
        devicePageCodeId(accessToken, "POWER", "Power", 2);
        int deviceA = createDevice(accessToken, "Widget-Same-A");
        int deviceB = createDevice(accessToken, "Widget-Same-B");

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode":"COOLING",
                                  "name":"현재값",
                                  "queryKind":"last",
                                  "deviceIds":[%d],
                                  "pointNames":["W"]
                                }
                                """.formatted(deviceA)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode":"POWER",
                                  "name":"현재값",
                                  "queryKind":"last",
                                  "deviceIds":[%d],
                                  "pointNames":["W"]
                                }
                                """.formatted(deviceB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageCode").value("POWER"));
    }

    @Test
    void createWidget_whenPageCodeMissing_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "widget-missing-page", "password123");
        int deviceId = createDevice(accessToken, "Widget-Missing-Page");

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode":"COOLING",
                                  "name":"칠러",
                                  "queryKind":"last",
                                  "deviceIds":[%d],
                                  "pointNames":["W"]
                                }
                                """.formatted(deviceId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DEVICE_PAGE code not found: COOLING"));
    }

    @Test
    void createWidget_whenQueryKindInvalid_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "widget-bad-kind", "password123");
        devicePageCodeId(accessToken, "COOLING", "Cooling", 1);
        int deviceId = createDevice(accessToken, "Widget-Bad-Kind");

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode":"COOLING",
                                  "name":"PUE",
                                  "queryKind":"pue",
                                  "deviceIds":[%d]
                                }
                                """.formatted(deviceId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("queryKind must be last, aggregate, or count"));
    }

    @Test
    void updateAndDeleteWidget() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "widget-upd", "password123");
        devicePageCodeId(accessToken, "COOLING", "Cooling", 1);
        int deviceA = createDevice(accessToken, "Widget-Upd-A");
        int deviceB = createDevice(accessToken, "Widget-Upd-B");

        String createResponse = mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode":"COOLING",
                                  "name":"칠러",
                                  "queryKind":"last",
                                  "deviceIds":[%d],
                                  "pointNames":["W"]
                                }
                                """.formatted(deviceA)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int id = objectMapper.readTree(createResponse).path("data").path("id").asInt();

        mockMvc.perform(put("/api/manager/widgets/{id}", id)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "칠러 상태",
                                  "enabled": false,
                                  "queryKind": "last",
                                  "deviceIds": [%d],
                                  "pointNames": ["status"]
                                }
                                """.formatted(deviceB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("칠러 상태"))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.deviceIds[0]").value(deviceB))
                .andExpect(jsonPath("$.data.pointNames[0]").value("status"));

        mockMvc.perform(get("/api/manager/widgets/{id}", id)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("칠러 상태"));

        mockMvc.perform(delete("/api/manager/widgets/{id}", id)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(id));

        mockMvc.perform(get("/api/manager/widgets")
                        .param("pageCode", "COOLING")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void replaceLayout_savesGridPosition() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "widget-layout", "password123");
        devicePageCodeId(accessToken, "dashboard", "dashboard", 1);
        int deviceId = createDevice(accessToken, "Widget-Layout");

        String createResponse = mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode": "dashboard",
                                  "name": "PDU 카드",
                                  "queryKind": "last",
                                  "deviceIds": [%d],
                                  "pointNames": ["W"],
                                  "layout": { "gridX": 0, "gridY": 0, "w": 2, "h": 1 }
                                }
                                """.formatted(deviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.layout.gridX").value(0))
                .andExpect(jsonPath("$.data.layout.gridY").value(0))
                .andExpect(jsonPath("$.data.layout.w").value(2))
                .andExpect(jsonPath("$.data.layout.h").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        int id = objectMapper.readTree(createResponse).path("data").path("id").asInt();

        mockMvc.perform(put("/api/manager/widgets/{id}/layout", id)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "gridX": 3, "gridY": 1, "w": 4, "h": 2 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.layout.gridX").value(3))
                .andExpect(jsonPath("$.data.layout.gridY").value(1))
                .andExpect(jsonPath("$.data.layout.w").value(4))
                .andExpect(jsonPath("$.data.layout.h").value(2));
    }

    private int createDevice(String accessToken, String name) throws Exception {
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
        int modelId = objectMapper.readTree(modelResponse).path("data").path("id").asInt();

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

        return objectMapper.readTree(deviceResponse).path("data").path("id").asInt();
    }

    @Test
    void toggleEnabled_andFilterByEnabled() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "widget-toggle", "password123");
        devicePageCodeId(accessToken, "dashboard", "Dashboard", 1);
        int deviceId = createDevice(accessToken, "Toggle-PDU");

        String createResponse = mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode": "dashboard",
                                  "name": "토글 테스트",
                                  "queryKind": "last",
                                  "deviceIds": [%d],
                                  "pointNames": ["W"]
                                }
                                """.formatted(deviceId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int widgetId = objectMapper.readTree(createResponse).path("data").path("id").asInt();

        mockMvc.perform(patch("/api/manager/widgets/" + widgetId + "/enabled")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/manager/widgets")
                        .param("pageCode", "dashboard")
                        .param("enabled", "true")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/manager/widgets")
                        .param("pageCode", "dashboard")
                        .param("enabled", "false")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(widgetId))
                .andExpect(jsonPath("$.data[0].enabled").value(false));
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
