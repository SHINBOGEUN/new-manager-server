package net.vivans.dcim.module.device.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import net.vivans.dcim.module.common.domain.repository.CodeGroupRepository;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
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
import static net.vivans.dcim.support.UnitCodeTestSupport.unitCodeId;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodeGroupRepository codeGroupRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    @Test
    void widgetFormSavesDesignWithContentAndPreservesItWhenOmitted() throws Exception {
        String token = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-design-form", "password123");
        devicePageCodeId(token, "DESIGN", "Design", 1);
        String created = mockMvc.perform(post("/api/manager/widgets").header("Authorization", bearerToken(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"pageCode":"DESIGN","name":"디자인 선택","queryKind":"count","countMode":"total","visualStyle":"vivid"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.visualStyle").value("vivid"))
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(created).path("data").path("id").asInt();
        mockMvc.perform(put("/api/manager/widgets/{id}", id).header("Authorization", bearerToken(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"디자인 변경","queryKind":"count","countMode":"total","visualStyle":"minimal"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.visualStyle").value("minimal"));
        mockMvc.perform(put("/api/manager/widgets/{id}", id).header("Authorization", bearerToken(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"이름만 변경","queryKind":"count","countMode":"total"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.visualStyle").value("minimal"));
        mockMvc.perform(put("/api/manager/widgets/{id}", id).header("Authorization", bearerToken(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"저장되면 안 됨","queryKind":"count","countMode":"total","visualStyle":"unknown"}
                        """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/manager/widgets/{id}", id).header("Authorization", bearerToken(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("이름만 변경"))
                .andExpect(jsonPath("$.data.visualStyle").value("minimal"));
    }

    @Test
    void visualStyleIsSavedAndReturnedForWidget() throws Exception {
        String token = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-style", "password123");
        devicePageCodeId(token, "STYLE", "Style", 1);
        String created = mockMvc.perform(post("/api/manager/widgets").header("Authorization", bearerToken(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pageCode\":\"STYLE\",\"name\":\"스타일 테스트\",\"queryKind\":\"count\",\"countMode\":\"total\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.visualStyle").value("classic"))
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(created).path("data").path("id").asInt();
        mockMvc.perform(patch("/api/manager/widgets/" + id + "/visual-style")
                .header("Authorization", bearerToken(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"style\":\"vivid\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.visualStyle").value("vivid"));
        mockMvc.perform(get("/api/manager/widgets/" + id).header("Authorization", bearerToken(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.visualStyle").value("vivid"));
        mockMvc.perform(patch("/api/manager/widgets/" + id + "/visual-style")
                .header("Authorization", bearerToken(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"style\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
    }


    @Test
    void createAndListWidgets_filtersByPageCode() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-create", "password123");
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
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-dup", "password123");
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
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-same-name", "password123");
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
    void createLastWidget_savesPointsPerDevice() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-last-sources", "password123");
        devicePageCodeId(accessToken, "COOLING", "Cooling", 1);
        int deviceA = createDevice(accessToken, "Widget-Last-A");
        int deviceB = createDevice(accessToken, "Widget-Last-B");

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode":"COOLING",
                                  "name":"장비별 현재값",
                                  "queryKind":"last",
                                  "lastSources":[
                                    {"deviceId":%d,"pointNames":["TEMP","HUM"]},
                                    {"deviceId":%d,"pointNames":["POWER"]}
                                  ]
                                }
                                """.formatted(deviceA, deviceB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceIds", hasSize(2)))
                .andExpect(jsonPath("$.data.lastSources", hasSize(2)))
                .andExpect(jsonPath("$.data.lastSources[0].deviceId").value(deviceA))
                .andExpect(jsonPath("$.data.lastSources[0].pointNames", hasSize(2)))
                .andExpect(jsonPath("$.data.lastSources[1].deviceId").value(deviceB))
                .andExpect(jsonPath("$.data.lastSources[1].pointNames[0]").value("POWER"));
    }

    @Test
    void createWidget_whenPageCodeMissing_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-missing-page", "password123");
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
    void updateAndDeleteWidget() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-upd", "password123");
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
    void updateWidget_movesPageAndRejectsDuplicateNameAtDestination() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-move-page", "password123");
        devicePageCodeId(accessToken, "COOLING", "Cooling", 1);
        devicePageCodeId(accessToken, "POWER", "Power", 2);
        int deviceId = createDevice(accessToken, "Widget-Move-Page");

        String created = mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"COOLING","name":"이동 대상","queryKind":"last",
                                 "deviceIds":[%d],"pointNames":["W"]}
                                """.formatted(deviceId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(created).path("data").path("id").asInt();

        mockMvc.perform(put("/api/manager/widgets/{id}", id)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"POWER","name":"이동 대상","queryKind":"last",
                                 "deviceIds":[%d],"pointNames":["W"]}
                                """.formatted(deviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageCode").value("POWER"));
        mockMvc.perform(get("/api/manager/widgets").param("pageCode", "COOLING")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get("/api/manager/widgets").param("pageCode", "POWER")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"COOLING","name":"이동 대상","queryKind":"last",
                                 "deviceIds":[%d],"pointNames":["W"]}
                                """.formatted(deviceId)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/manager/widgets/{id}", id)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"COOLING","name":"이동 대상","queryKind":"last",
                                 "deviceIds":[%d],"pointNames":["W"]}
                                """.formatted(deviceId)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/manager/widgets/{id}", id)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.pageCode").value("POWER"));
    }

    @Test
    void replaceLayout_savesGridPosition() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-layout", "password123");
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

    @Test
    void calculatedWidget_createUpdateToggleAndDelete() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-pue", "password123");
        devicePageCodeId(accessToken, "dashboard", "Dashboard", 1);
        devicePageCodeId(accessToken, "POWER", "Power", 2);
        int totalDevice = createDevice(accessToken, "Pue-Total");
        int coolerDevice = createDevice(accessToken, "Pue-Cooler");

        Integer protocolGroupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer snmpId = findOrCreateCommonCode(accessToken, protocolGroupId, "snmp", "SNMP", 1);
        for (int deviceId : new int[]{totalDevice, coolerDevice}) {
            mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                            .header("Authorization", bearerToken(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"protocolTypeId":%d,"host":"127.0.0.1","port":%d,"enabled":true}
                                    """.formatted(snmpId, 16000 + deviceId)))
                    .andExpect(status().isOk());
        }
        String definitionResponse = mockMvc.perform(post("/api/manager/calculated-metrics")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"사업장 계산 지표","formula":"FACILITY / IT","resultUnit":"PUE",
                                 "sources":[{"deviceId":%d,"alias":"FACILITY","pointName":"TOTAL_WT","protocol":"snmp"},
                                            {"deviceId":%d,"alias":"IT","pointName":"TOTAL_WT","protocol":"snmp"}]}
                                """.formatted(totalDevice, coolerDevice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.formula").value("FACILITY / IT"))
                .andReturn().getResponse().getContentAsString();
        int definitionId = objectMapper.readTree(definitionResponse).path("data").path("id").asInt();

        mockMvc.perform(post("/api/manager/calculated-metrics")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"MQTT 제외 확인","formula":"LORA","sources":[
                                  {"deviceId":%d,"alias":"LORA","pointName":"TEMP","protocol":"mqtt"}]}
                                """.formatted(totalDevice)))
                .andExpect(status().isBadRequest());

        String created = mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"dashboard","name":"사업장 계산 지표","queryKind":"calculated",
                                 "pueDefinitionId":%d,"pueRangePreset":"last_24h","pueFreshnessMinutes":30}
                                """.formatted(definitionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.queryKind").value("calculated"))
                .andExpect(jsonPath("$.data.pueFreshnessMinutes").value(30))
                .andExpect(jsonPath("$.data.pueSources", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        int widgetId = objectMapper.readTree(created).path("data").path("id").asInt();

        mockMvc.perform(put("/api/manager/calculated-metrics/{id}", definitionId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"사업장 계산 지표","formula":"FACILITY - IT","resultUnit":"W",
                                 "sources":[{"deviceId":%d,"alias":"FACILITY","pointName":"TOTAL_WT","protocol":"snmp"},
                                            {"deviceId":%d,"alias":"IT","pointName":"TOTAL_WT","protocol":"snmp"}]}
                                """.formatted(totalDevice, coolerDevice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.formula").value("FACILITY - IT"))
                .andExpect(jsonPath("$.data.configVersion").value(2));

        mockMvc.perform(put("/api/manager/widgets/{id}", widgetId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"POWER","name":"사업장 계산 지표 수정","enabled":true,"queryKind":"calculated",
                                 "pueDefinitionId":%d,"pueRangePreset":"today","pueFreshnessMinutes":60}
                                """.formatted(definitionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("사업장 계산 지표 수정"))
                .andExpect(jsonPath("$.data.pageCode").value("POWER"))
                .andExpect(jsonPath("$.data.pueFreshnessMinutes").value(60));

        mockMvc.perform(patch("/api/manager/widgets/{id}/enabled", widgetId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
        mockMvc.perform(delete("/api/manager/widgets/{id}", widgetId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/manager/widgets/{id}", widgetId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void psychrometricWidget_createAndUpdate_keepsExistingSources() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-psychrometric", "password123");
        devicePageCodeId(accessToken, "dashboard", "Dashboard", 1);
        devicePageCodeId(accessToken, "COOLING", "Cooling", 2);
        int temperatureDevice = createDevice(accessToken, "Psych-Temperature");
        int humidityDevice = createDevice(accessToken, "Psych-Humidity");

        String created = mockMvc.perform(post("/api/manager/widgets/psychrometric")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"dashboard","name":"센터 온습도","enabled":true,"visualStyle":"vivid",
                                 "temperatureSources":[{"deviceId":%d,"pointName":"TEMP"}],
                                 "humiditySources":[{"deviceId":%d,"pointName":"HUM"}]}
                                """.formatted(temperatureDevice, humidityDevice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.queryKind").value("psychrometric"))
                .andExpect(jsonPath("$.data.visualStyle").value("vivid"))
                .andExpect(jsonPath("$.data.psychrometricSources", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        int widgetId = objectMapper.readTree(created).path("data").path("id").asInt();

        mockMvc.perform(put("/api/manager/widgets/{id}/psychrometric", widgetId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"COOLING","name":"센터 온습도 수정","enabled":true,"visualStyle":"minimal",
                                 "temperatureSources":[{"deviceId":%d,"pointName":"TEMP"}],
                                 "humiditySources":[{"deviceId":%d,"pointName":"HUM"}]}
                                """.formatted(temperatureDevice, humidityDevice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("센터 온습도 수정"))
                .andExpect(jsonPath("$.data.visualStyle").value("minimal"))
                .andExpect(jsonPath("$.data.pageCode").value("COOLING"))
                .andExpect(jsonPath("$.data.psychrometricSources", hasSize(2)));
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
        int protocolId = objectMapper.readTree(modelResponse).path("data").path("protocols").get(0).path("id").asInt();
        Integer dataPointTypeGroupId = findOrCreateCodeGroup(accessToken, "DATA_POINT_TYPE", "Data Point Type");
        Integer powerTypeId = findOrCreateCommonCode(accessToken, dataPointTypeGroupId, "POWER", "전력", 1);
        mockMvc.perform(post("/api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points", modelId, protocolId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataPointTypeId":%d,"name":"TOTAL_WT","oid":".1.3.6.1.4.1.9999.1","unitCodeId":%d,"enabled":true}
                                """.formatted(powerTypeId, unitCodeId("W", codeGroupRepository, commonCodeRepository))))
                .andExpect(status().isOk());

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
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "widget-toggle", "password123");
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
