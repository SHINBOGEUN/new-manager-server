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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
class CollectionTaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTask_withPeriodGroups_generatesSpecPerGroup() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-task-create", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, "V4-AP8959", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.1.0", "temp", "C");
        String locationCode = createRootLocation(accessToken, "V4-Create-Loc");
        int device1 = createDevice(accessToken, modelId, locationCode, "V4-DEV-1");
        int device2 = createDevice(accessToken, modelId, locationCode, "V4-DEV-2");
        int device3 = createDevice(accessToken, modelId, locationCode, "V4-DEV-3");
        createEndpoint(accessToken, device1, snmpId, "10.88.10.1", 161);
        createEndpoint(accessToken, device2, snmpId, "10.88.10.2", 161);
        createEndpoint(accessToken, device3, snmpId, "10.88.10.3", 161);

        mockMvc.perform(post("/api/manager/collector/tasks")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "AP8959 SNMP 수집",
                                  "modelId": %d,
                                  "scriptTypeId": %d,
                                  "active": true,
                                  "groups": [
                                    {
                                      "name": "1분 그룹",
                                      "cronExpression": "0 */1 * * * *",
                                      "deviceIds": [%d, %d]
                                    },
                                    {
                                      "name": "5분 그룹",
                                      "cronExpression": "0 */5 * * * *",
                                      "deviceIds": [%d]
                                    }
                                  ]
                                }
                                """.formatted(modelId, snmpId, device1, device2, device3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("AP8959 SNMP 수집"))
                .andExpect(jsonPath("$.data.modelId").value(modelId))
                .andExpect(jsonPath("$.data.scriptTypeCode").value("snmp"))
                .andExpect(jsonPath("$.data.groups", hasSize(2)))
                .andExpect(jsonPath("$.data.groups[0].cronExpression").value("0 */1 * * * *"))
                .andExpect(jsonPath("$.data.groups[0].devices", hasSize(2)))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("10.88.10.1")))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("10.88.10.2")))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(not(containsString("10.88.10.3"))))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.1.0")))
                .andExpect(jsonPath("$.data.groups[1].cronExpression").value("0 */5 * * * *"))
                .andExpect(jsonPath("$.data.groups[1].generatedSpec").value(containsString("10.88.10.3")))
                .andExpect(jsonPath("$.data.groups[1].generatedSpec").value(not(containsString("10.88.10.1"))));
    }

    @Test
    void createTask_withNonProtocolTypeGroup_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-task-bad-group", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, "V4-BAD-GROUP", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.9.0", "temp", "C");
        Integer devicePageGroupId = findOrCreateCodeGroup(accessToken, "DEVICE_PAGE", "Device Page");
        Integer pageCodeId = findOrCreateCommonCode(accessToken, devicePageGroupId, "ENVIRONMENT", "Environment", 1);

        mockMvc.perform(post("/api/manager/collector/tasks")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "잘못된 타입",
                                  "modelId": %d,
                                  "scriptTypeId": %d,
                                  "active": true
                                }
                                """.formatted(modelId, pageCodeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("scriptType must belong to PROTOCOL_TYPE group"));
    }

    @Test
    void createTask_duplicateModelAndScriptType_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-task-dup", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, "V4-DUP", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.8.0", "temp", "C");
        createTask(accessToken, "첫번째", modelId, snmpId);

        mockMvc.perform(post("/api/manager/collector/tasks")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "두번째",
                                  "modelId": %d,
                                  "scriptTypeId": %d,
                                  "active": true
                                }
                                """.formatted(modelId, snmpId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("collection task already exists for this model and script type"));
    }

    @Test
    void createTask_deviceFromOtherModel_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-task-other-model", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelA = createDeviceModelWithSnmpPoint(
                accessToken, "V4-MODEL-A", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.7.0", "temp", "C");
        Integer modelB = createDeviceModelWithSnmpPoint(
                accessToken, "V4-MODEL-B", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.6.0", "hum", "%");
        String locationCode = createRootLocation(accessToken, "V4-Other-Loc");
        int otherDevice = createDevice(accessToken, modelB, locationCode, "V4-OTHER-DEV");

        mockMvc.perform(post("/api/manager/collector/tasks")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "모델 불일치",
                                  "modelId": %d,
                                  "scriptTypeId": %d,
                                  "groups": [
                                    {
                                      "name": "1분 그룹",
                                      "cronExpression": "0 */1 * * * *",
                                      "deviceIds": [%d]
                                    }
                                  ]
                                }
                                """.formatted(modelA, snmpId, otherDevice)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("device does not belong to task model"));
    }

    @Test
    void createTask_sameDeviceInTwoGroups_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-task-dup-device", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, "V4-DUP-DEV", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.5.0", "temp", "C");
        String locationCode = createRootLocation(accessToken, "V4-DupDev-Loc");
        int deviceId = createDevice(accessToken, modelId, locationCode, "V4-SHARED");

        mockMvc.perform(post("/api/manager/collector/tasks")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "장비 중복",
                                  "modelId": %d,
                                  "scriptTypeId": %d,
                                  "groups": [
                                    {
                                      "name": "1분 그룹",
                                      "cronExpression": "0 */1 * * * *",
                                      "deviceIds": [%d]
                                    },
                                    {
                                      "name": "5분 그룹",
                                      "cronExpression": "0 */5 * * * *",
                                      "deviceIds": [%d]
                                    }
                                  ]
                                }
                                """.formatted(modelId, snmpId, deviceId, deviceId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("device already belongs to another group in this task"));
    }

    @Test
    void getTasks_filterByModelId_returnsMatchingTasks() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-task-filter", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelA = createDeviceModelWithSnmpPoint(
                accessToken, "V4-FILTER-A", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.4.0", "temp", "C");
        Integer modelB = createDeviceModelWithSnmpPoint(
                accessToken, "V4-FILTER-B", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.3.0", "hum", "%");
        createTask(accessToken, "모델A 수집", modelA, snmpId);
        createTask(accessToken, "모델B 수집", modelB, snmpId);

        mockMvc.perform(get("/api/manager/collector/tasks")
                        .param("modelId", String.valueOf(modelA))
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("모델A 수집"))
                .andExpect(jsonPath("$.data[0].modelId").value(modelA));
    }

    @Test
    void updateTask_updatesNameAndActiveOnly() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-task-update", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, "V4-UPDATE", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.2.0", "temp", "C");
        int taskId = createTask(accessToken, "수집 작업", modelId, snmpId);

        mockMvc.perform(put("/api/manager/collector/tasks/{taskId}", taskId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "수집 작업 수정",
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.name").value("수집 작업 수정"))
                .andExpect(jsonPath("$.data.modelId").value(modelId))
                .andExpect(jsonPath("$.data.scriptTypeId").value(snmpId))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void toggleTask_flipsActiveFlag() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-task-toggle", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, "V4-TOGGLE", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.11.1.0", "temp", "C");
        int taskId = createTask(accessToken, "비활성 전환 대상", modelId, snmpId);

        mockMvc.perform(patch("/api/manager/collector/tasks/{taskId}/toggle", taskId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void createDevice_whenModelTaskExists_assignsToDefaultGroupAndUpdatesSpec() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-auto-assign-device", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, "V4-AUTO-DEV", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.12.1.0", "temp", "C");
        int taskId = createTask(accessToken, "자동 편입 Task", modelId, snmpId);
        String locationCode = createRootLocation(accessToken, "V4-Auto-Loc");
        int deviceId = createDevice(accessToken, modelId, locationCode, "V4-AUTO-1");

        mockMvc.perform(get("/api/manager/collector/tasks/{taskId}", taskId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups", hasSize(1)))
                .andExpect(jsonPath("$.data.groups[0].devices", hasSize(1)))
                .andExpect(jsonPath("$.data.groups[0].devices[0].deviceId").value(deviceId))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(not(containsString("10.88.30.1"))));

        createEndpoint(accessToken, deviceId, snmpId, "10.88.30.1", 161);

        mockMvc.perform(get("/api/manager/collector/tasks/{taskId}", taskId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("10.88.30.1")))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.12.1.0")));
    }

    @Test
    void createTask_assignsExistingModelDevicesToDefaultGroup() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-auto-assign-task", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, "V4-AUTO-TASK", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.12.2.0", "hum", "%");
        String locationCode = createRootLocation(accessToken, "V4-Auto-Task-Loc");
        int deviceId = createDevice(accessToken, modelId, locationCode, "V4-EXISTING");
        createEndpoint(accessToken, deviceId, snmpId, "10.88.30.2", 161);

        mockMvc.perform(post("/api/manager/collector/tasks")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "기존 장비 편입",
                                  "modelId": %d,
                                  "scriptTypeId": %d,
                                  "active": true
                                }
                                """.formatted(modelId, snmpId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups", hasSize(1)))
                .andExpect(jsonPath("$.data.groups[0].name").value("기본 그룹"))
                .andExpect(jsonPath("$.data.groups[0].devices[0].deviceId").value(deviceId))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("10.88.30.2")));
    }

    @Test
    void deleteDevice_removesDeviceFromGeneratedSpec() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-auto-delete-device", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, "V4-AUTO-DEL", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.12.3.0", "temp", "C");
        String locationCode = createRootLocation(accessToken, "V4-Auto-Del-Loc");
        int keepDeviceId = createDevice(accessToken, modelId, locationCode, "V4-KEEP-DEL");
        int deleteDeviceId = createDevice(accessToken, modelId, locationCode, "V4-GONE");
        createEndpoint(accessToken, keepDeviceId, snmpId, "10.88.30.3", 161);
        createEndpoint(accessToken, deleteDeviceId, snmpId, "10.88.30.4", 161);
        int taskId = createTask(accessToken, "삭제 연동", modelId, snmpId);

        mockMvc.perform(get("/api/manager/collector/tasks/{taskId}", taskId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("10.88.30.3")))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("10.88.30.4")));

        mockMvc.perform(delete("/api/manager/devices/{deviceId}", deleteDeviceId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manager/collector/tasks/{taskId}", taskId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups[0].devices", hasSize(1)))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("10.88.30.3")))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(not(containsString("10.88.30.4"))));
    }

    @Test
    void updateGroup_keepsOverlappingDeviceWithoutDuplicateMappingError() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-group-keep-device", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, "V4-KEEP-MAP", "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.12.8.0", "temp", "C");
        String locationCode = createRootLocation(accessToken, "V4-Keep-Map-Loc");
        int keepId = createDevice(accessToken, modelId, locationCode, "V4-KEEP-ONLY");
        int dropA = createDevice(accessToken, modelId, locationCode, "V4-DROP-A");
        int dropB = createDevice(accessToken, modelId, locationCode, "V4-DROP-B");
        int taskId = createTaskWithDevices(
                accessToken, "매핑 유지", modelId, snmpId, "0 */5 * * * *", keepId, dropA, dropB);

        String taskJson = mockMvc.perform(get("/api/manager/collector/tasks/{taskId}", taskId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int groupId = objectMapper.readTree(taskJson).path("data").path("groups").get(0).path("id").asInt();

        mockMvc.perform(put("/api/manager/collector/tasks/{taskId}/groups/{groupId}", taskId, groupId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "5분 그룹",
                                  "cronExpression": "0 */5 * * * *",
                                  "deviceIds": [%d],
                                  "active": true
                                }
                                """.formatted(keepId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.devices", hasSize(1)))
                .andExpect(jsonPath("$.data.devices[0].deviceId").value(keepId));
    }

    @Test
    void disableEndpoint_dropsDeviceFromGeneratedSpec() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "v4-task-endpoint-off", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, "V4-SCRIPT-OFF", "Dragino", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.10.2.0", "hum", "%");
        String locationCode = createRootLocation(accessToken, "V4-Script-Off-Loc");
        int keepDeviceId = createDevice(accessToken, modelId, locationCode, "V4-KEEP");
        int offDeviceId = createDevice(accessToken, modelId, locationCode, "V4-OFF");
        createEndpoint(accessToken, keepDeviceId, snmpId, "10.88.2.10", 161);
        int offEndpointId = createEndpoint(accessToken, offDeviceId, snmpId, "10.88.2.11", 161);

        int taskId = createTaskWithDevices(
                accessToken, "SNMP on/off", modelId, snmpId, "0 */1 * * * *", keepDeviceId, offDeviceId);

        mockMvc.perform(get("/api/manager/collector/tasks/{taskId}", taskId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("10.88.2.10")))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("10.88.2.11")));

        mockMvc.perform(put("/api/manager/devices/{deviceId}/endpoints/{endpointId}", offDeviceId, offEndpointId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "10.88.2.11",
                                  "port": 161,
                                  "enabled": false
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manager/collector/tasks/{taskId}", taskId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(containsString("10.88.2.10")))
                .andExpect(jsonPath("$.data.groups[0].generatedSpec").value(not(containsString("10.88.2.11"))));
    }

    private Integer scriptTypeId(String accessToken, String code, String name, int sortOrder) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        return findOrCreateCommonCode(accessToken, groupId, code, name, sortOrder);
    }

    private int createTask(String accessToken, String name, Integer modelId, Integer scriptTypeId) throws Exception {
        String response = mockMvc.perform(post("/api/manager/collector/tasks")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "modelId": %d,
                                  "scriptTypeId": %d,
                                  "active": true
                                }
                                """.formatted(name, modelId, scriptTypeId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private int createTaskWithDevices(
            String accessToken,
            String name,
            Integer modelId,
            Integer scriptTypeId,
            String cronExpression,
            int... deviceIds
    ) throws Exception {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < deviceIds.length; i++) {
            if (i > 0) {
                ids.append(", ");
            }
            ids.append(deviceIds[i]);
        }
        String response = mockMvc.perform(post("/api/manager/collector/tasks")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "modelId": %d,
                                  "scriptTypeId": %d,
                                  "active": true,
                                  "groups": [
                                    {
                                      "name": "기본 그룹",
                                      "cronExpression": "%s",
                                      "deviceIds": [%s]
                                    }
                                  ]
                                }
                                """.formatted(name, modelId, scriptTypeId, cronExpression, ids)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
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

    private Integer createDeviceModelWithSnmpPoint(
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
        Integer deviceTypeId = findOrCreateCommonCode(accessToken, modelTypeGroupId, "SENSOR", "Sensor", 2);

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

    private String createRootLocation(String accessToken, String name) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer zoneTypeId = findOrCreateCommonCode(accessToken, groupId, "ZONE", "Zone", 1);
        String response = mockMvc.perform(post("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCode": null, "locationTypeId": %d, "name": "%s"}
                                """.formatted(zoneTypeId, name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("code").asText();
    }
}
