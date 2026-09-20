package net.vivans.dcim.module.operations.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import net.vivans.dcim.bootstrap.ManagerServerApplication;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorJobClient;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorJobResponse;
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
import java.util.NoSuchElementException;

import static net.vivans.dcim.support.AuthTestSupport.bearerToken;
import static net.vivans.dcim.support.AuthTestSupport.loginAndGetAccessToken;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "수집 안정성 강화" 작업 중 요구사항 #3(실패 시각/횟수/원인 가시화)과 #5(정상 복구 자동 반영)를
 * 검증한다. 실제 Collector 대신 CollectorJobClient를 목으로 대체해, Collector가 보고하는
 * job 상태(성공/실패 이력)가 /api/manager/operations/collection/jobs 응답에
 * NOT_SYNCED / UNKNOWN / FAILING / RECOVERED / NORMAL 로 정확히 반영되는지 확인한다.
 */
@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class CollectionOperationsControllerJobHealthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollectionTaskRepository collectionTaskRepository;

    @MockitoBean
    private CollectorJobClient collectorJobClient;

    @Test
    void jobHealth_reflectsNotSyncedUnknownFailingAndRecoveredStates() throws Exception {
        // 픽스처를 만드는 동안은 실제 동기화가 일어나지 않도록 비활성 상태로 둔다.
        when(collectorJobClient.isEnabled()).thenReturn(false);

        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "ops-job-health", "password123");
        Integer snmpId = scriptTypeId(accessToken, "snmp", "SNMP", 1);

        int notSyncedTaskId = createTaskWithModel(accessToken, snmpId, "OPS-JH-NOTSYNCED", "동기화 전");
        int unknownTaskId = createTaskWithModel(accessToken, snmpId, "OPS-JH-UNKNOWN", "Collector 미확인");
        int failingTaskId = createTaskWithModel(accessToken, snmpId, "OPS-JH-FAILING", "실패중");
        int recoveredTaskId = createTaskWithModel(accessToken, snmpId, "OPS-JH-RECOVERED", "복구됨");

        int notSyncedGroupId = defaultGroupId(accessToken, notSyncedTaskId);
        int unknownGroupId = defaultGroupId(accessToken, unknownTaskId);
        int failingGroupId = defaultGroupId(accessToken, failingTaskId);
        int recoveredGroupId = defaultGroupId(accessToken, recoveredTaskId);

        // Collector에 실제로 등록된 것처럼 collectorJobId를 직접 채워 넣는다(동기화 자체는 이 테스트의 관심사가 아님).
        assignCollectorJobId(unknownTaskId, unknownGroupId, "job-unknown");
        assignCollectorJobId(failingTaskId, failingGroupId, "job-failing");
        assignCollectorJobId(recoveredTaskId, recoveredGroupId, "job-recovered");

        Instant pastFailure = Instant.now().minusSeconds(120);
        Instant recentSuccess = Instant.now();

        when(collectorJobClient.isEnabled()).thenReturn(true);
        when(collectorJobClient.list()).thenReturn(List.of(
                // job-unknown은 목록에 없음 -> UNKNOWN
                new CollectorJobResponse(
                        "job-failing", failingTaskId, failingGroupId, null, "snmp", "0 */1 * * * *",
                        true, 1, null, pastFailure, 3, "deviceId=1 host=10.0.0.1:161 timeout"),
                new CollectorJobResponse(
                        "job-recovered", recoveredTaskId, recoveredGroupId, null, "snmp", "0 */1 * * * *",
                        true, 1, recentSuccess, pastFailure, 0, "deviceId=2 host=10.0.0.2:161 timeout")
        ));

        mockMvc.perform(get("/api/manager/operations/collection/jobs")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(groupJson(notSyncedGroupId) + ".status").value("NOT_SYNCED"))
                // JSONPath 필터([?(@.groupId == N)])는 결과를 항상 배열로 돌려주므로,
                // null 값도 "[null]" 형태로 비교된다. nullValue() 단독으로는 타입이 안 맞아
                // 실패하므로 hasItem(nullValue())로 "배열 안에 null이 있다"를 검증한다.
                .andExpect(jsonPath(groupJson(notSyncedGroupId) + ".collectorJobId")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath(groupJson(unknownGroupId) + ".status").value("UNKNOWN"))
                .andExpect(jsonPath(groupJson(failingGroupId) + ".status").value("FAILING"))
                .andExpect(jsonPath(groupJson(failingGroupId) + ".consecutiveFailureCount").value(3))
                // 위와 같은 이유로, containsString()도 Matcher 오버로드라 배열 언래핑이 되지 않는다.
                // hasItem(containsString(...))로 "배열의 한 원소가 이 문자열을 포함한다"를 검증한다.
                .andExpect(jsonPath(groupJson(failingGroupId) + ".lastFailureReason").value(
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("timeout"))))
                .andExpect(jsonPath(groupJson(recoveredGroupId) + ".status").value("RECOVERED"))
                .andExpect(jsonPath(groupJson(recoveredGroupId) + ".consecutiveFailureCount").value(0))
                .andExpect(jsonPath(groupJson(recoveredGroupId) + ".lastSuccessAt").exists());
    }

    /** data 배열에서 groupId로 특정 원소를 찾는 jsonPath. 배열 순서에 의존하지 않기 위함. */
    private static String groupJson(int groupId) {
        return "$.data[?(@.groupId == " + groupId + ")]";
    }

    private void assignCollectorJobId(int taskId, int groupId, String collectorJobId) {
        CollectionTask task = collectionTaskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("task not found: " + taskId));
        CollectionTaskGroup group = task.getGroups().stream()
                .filter(candidate -> candidate.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("group not found: " + groupId));
        group.updateCollectorJobId(collectorJobId);
        collectionTaskRepository.saveAndFlush(task);
    }

    private int defaultGroupId(String accessToken, int taskId) throws Exception {
        String response = mockMvc.perform(get("/api/manager/collector/tasks/{taskId}", taskId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("groups").get(0).path("id").asInt();
    }

    private int createTaskWithModel(String accessToken, Integer snmpId, String modelName, String taskName) throws Exception {
        Integer modelId = createDeviceModelWithSnmpPoint(
                accessToken, modelName, "APC", snmpId, false,
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.99." + Math.abs(modelName.hashCode() % 1000) + ".0",
                "temp", "C");
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
                                """.formatted(taskName, modelId, snmpId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private Integer scriptTypeId(String accessToken, String code, String name, int sortOrder) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
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
            String accessToken, Integer groupId, String code, String name, Integer sortOrder
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
}
