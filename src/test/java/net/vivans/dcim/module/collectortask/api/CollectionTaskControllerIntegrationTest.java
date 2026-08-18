package net.vivans.dcim.module.collectortask.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class CollectionTaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTask_returnsScriptTypeFields() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "collection-task-create", "password123");
        Integer scriptTypeId = scriptTypeId(accessToken, "snmp", "SNMP", 1);

        mockMvc.perform(post("/api/manager/collector/tasks")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "전체 SNMP 수집",
                                  "cronExpression": "0 */5 * * * *",
                                  "scriptTypeId": %d,
                                  "active": true
                                }
                                """.formatted(scriptTypeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.name").value("전체 SNMP 수집"))
                .andExpect(jsonPath("$.data.cronExpression").value("0 */5 * * * *"))
                .andExpect(jsonPath("$.data.scriptTypeId").value(scriptTypeId))
                .andExpect(jsonPath("$.data.scriptTypeCode").value("snmp"))
                .andExpect(jsonPath("$.data.scriptTypeName").value("SNMP"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void createTask_withNonProtocolTypeGroup_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "collection-task-bad-group", "password123");
        Integer devicePageGroupId = findOrCreateCodeGroup(accessToken, "DEVICE_PAGE", "Device Page");
        Integer pageCodeId = findOrCreateCommonCode(accessToken, devicePageGroupId, "ENVIRONMENT", "Environment", 1);

        mockMvc.perform(post("/api/manager/collector/tasks")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "잘못된 타입",
                                  "cronExpression": "0 */5 * * * *",
                                  "scriptTypeId": %d,
                                  "active": true
                                }
                                """.formatted(pageCodeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("scriptType must belong to PROTOCOL_TYPE group"));
    }

    @Test
    void getTasks_filterByScriptTypeId_returnsMatchingTasks() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "collection-task-filter", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modbusId = scriptTypeId(accessToken, "modbus", "Modbus", 2);

        createTask(accessToken, "SNMP 수집", "0 */5 * * * *", snmpId, true);
        createTask(accessToken, "Modbus 수집", "0 */10 * * * *", modbusId, true);

        mockMvc.perform(get("/api/manager/collector/tasks")
                        .param("scriptTypeId", String.valueOf(snmpId))
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("SNMP 수집"))
                .andExpect(jsonPath("$.data[0].scriptTypeId").value(snmpId))
                .andExpect(jsonPath("$.data[0].scriptTypeCode").value("snmp"));
    }

    @Test
    void updateTask_updatesScriptTypeAndCronExpression() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "collection-task-update", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modbusId = scriptTypeId(accessToken, "modbus", "Modbus", 2);
        String taskId = createTask(accessToken, "수집 작업", "0 */5 * * * *", snmpId, true);

        mockMvc.perform(put("/api/manager/collector/tasks/{taskId}", taskId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "수집 작업 수정",
                                  "cronExpression": "0 */10 * * * *",
                                  "scriptTypeId": %d,
                                  "active": false
                                }
                                """.formatted(modbusId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.name").value("수집 작업 수정"))
                .andExpect(jsonPath("$.data.cronExpression").value("0 */10 * * * *"))
                .andExpect(jsonPath("$.data.scriptTypeId").value(modbusId))
                .andExpect(jsonPath("$.data.scriptTypeCode").value("modbus"))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void toggleTask_flipsActiveFlag() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "collection-task-toggle", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        String taskId = createTask(accessToken, "비활성 전환 대상", "0 */5 * * * *", snmpId, true);

        mockMvc.perform(patch("/api/manager/collector/tasks/{taskId}/toggle", taskId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    private Integer scriptTypeId(String accessToken, String code, String name, int sortOrder) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        return findOrCreateCommonCode(accessToken, groupId, code, name, sortOrder);
    }

    private String createTask(String accessToken, String name, String cronExpression, Integer scriptTypeId, boolean active) throws Exception {
        String response = mockMvc.perform(post("/api/manager/collector/tasks")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "cronExpression": "%s",
                                  "scriptTypeId": %d,
                                  "active": %s
                                }
                                """.formatted(name, cronExpression, scriptTypeId, active)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asText();
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
