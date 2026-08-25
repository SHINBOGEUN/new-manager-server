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

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode": "COOLING",
                                  "name": "칠러",
                                  "queryKind": "last",
                                  "pointNames": ["status", "W"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageCode").value("COOLING"))
                .andExpect(jsonPath("$.data.name").value("칠러"))
                .andExpect(jsonPath("$.data.queryKind").value("last"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.pointNames[0]").value("status"));

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode": "POWER",
                                  "name": "전체 전력",
                                  "queryKind": "last",
                                  "pointNames": ["W"]
                                }
                                """))
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

        String body = """
                {
                  "pageCode": "COOLING",
                  "name": "칠러",
                  "queryKind": "last"
                }
                """;
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

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"COOLING","name":"현재값","queryKind":"last"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"POWER","name":"현재값","queryKind":"last"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageCode").value("POWER"));
    }

    @Test
    void createWidget_whenPageCodeMissing_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "widget-missing-page", "password123");

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"COOLING","name":"칠러","queryKind":"last"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DEVICE_PAGE code not found: COOLING"));
    }

    @Test
    void createWidget_whenQueryKindInvalid_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "widget-bad-kind", "password123");
        devicePageCodeId(accessToken, "COOLING", "Cooling", 1);

        mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"COOLING","name":"PUE","queryKind":"pue"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("queryKind must be last, aggregate, or count"));
    }

    @Test
    void updateAndDeleteWidget() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "widget-upd", "password123");
        devicePageCodeId(accessToken, "COOLING", "Cooling", 1);

        String createResponse = mockMvc.perform(post("/api/manager/widgets")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCode":"COOLING","name":"칠러","queryKind":"last"}
                                """))
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
                                  "pointNames": ["status"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("칠러 상태"))
                .andExpect(jsonPath("$.data.enabled").value(false))
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
