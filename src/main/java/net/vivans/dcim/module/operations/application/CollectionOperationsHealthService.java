package net.vivans.dcim.module.operations.application;

import com.influxdb.client.InfluxDBClient;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionGroupSpecService;
import net.vivans.dcim.module.collectortask.application.CollectorSyncService;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorHealthResponse;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorJobClient;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorJobResponse;
import net.vivans.dcim.module.operations.api.dto.CollectionJobHealthResponse;
import net.vivans.dcim.module.operations.api.dto.CollectionOperationsHealthResponse;
import net.vivans.dcim.module.operations.api.dto.CollectionReconciliationResponse;
import net.vivans.dcim.module.operations.config.OperationsHealthProperties;
import net.vivans.dcim.module.calculated.application.CalculatedMetricCollectorSyncService;
import net.vivans.dcim.module.calculated.domain.repository.CalculatedMetricRepository;
import net.vivans.dcim.module.query.config.InfluxProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionOperationsHealthService {

    private final DataSource dataSource;
    private final ObjectProvider<InfluxDBClient> influxClientProvider;
    private final InfluxProperties influxProperties;
    private final CollectorJobClient collectorJobClient;
    private final CollectionTaskRepository collectionTaskRepository;
    private final CollectorSyncService collectorSyncService;
    private final CalculatedMetricCollectorSyncService calculatedMetricCollectorSyncService;
    private final CalculatedMetricRepository calculatedMetricRepository;
    private final OperationsHealthProperties properties;

    @Transactional(readOnly = true)
    public CollectionOperationsHealthResponse getHealth() {
        List<CollectionOperationsHealthResponse.Component> components = new ArrayList<>();
        components.add(managerComponent());
        components.add(databaseComponent());
        components.add(influxComponent());

        CollectorHealthResponse collectorHealth = null;
        if (!collectorJobClient.isEnabled()) {
            components.add(component("collector", "Collector", "DISABLED", null,
                    "Collector 작업 동기화가 비활성화되어 있습니다.", Map.of()));
        } else {
            try {
                collectorHealth = collectorJobClient.health();
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("instanceId", collectorHealth.instanceId());
                details.put("jobs", collectorHealth.jobs());
                Object ingestEnabled = collectorHealth.details() == null ? null : collectorHealth.details().get("ingestEnabled");
                Object ingestConnected = collectorHealth.details() == null ? null : collectorHealth.details().get("ingestConnected");
                if (ingestEnabled != null) details.put("ingestEnabled", ingestEnabled);
                if (ingestConnected != null) details.put("ingestConnected", ingestConnected);
                components.add(component("collector", "Collector", "UP", null,
                        collectorMessage(ingestEnabled, ingestConnected), details));
            } catch (Exception exception) {
                components.add(component("collector", "Collector", "DOWN", null,
                        readableMessage(exception), Map.of()));
            }
        }

        components.add(mqttComponent());
        return new CollectionOperationsHealthResponse(Instant.now(), components, syncSummary(collectorHealth));
    }

    @Transactional
    public CollectionReconciliationResponse reconcile() {
        int synchronizedGroups = collectorSyncService.reconcileActiveGroups();
        int activeCalculatedMetrics = (int) calculatedMetricRepository.findAllByCollectionEnabled(true).size();
        calculatedMetricCollectorSyncService.repushActiveDefinitions();
        return new CollectionReconciliationResponse(
                Instant.now(),
                synchronizedGroups,
                activeCalculatedMetrics,
                "활성 수집 그룹과 PUE 정의를 Collector에 다시 동기화했습니다."
        );
    }

    /**
     * SNMP 수집 그룹(Collector job)별 최근 실패/복구 상태를 조회한다.
     * Collector에서 job 목록을 한 번만 받아 온 뒤 DB에 저장된 활성 그룹과 collectorJobId로 매칭한다.
     * Collector가 응답하지 않아도(재시작 직후 등) 예외를 던지지 않고 UNKNOWN 상태로 표시한다.
     */
    @Transactional(readOnly = true)
    public List<CollectionJobHealthResponse> getJobHealth() {
        if (!collectorJobClient.isEnabled()) {
            return List.of();
        }
        Map<String, CollectorJobResponse> byCollectorJobId;
        try {
            byCollectorJobId = collectorJobClient.list().stream()
                    .collect(Collectors.toMap(CollectorJobResponse::collectorJobId, job -> job, (a, b) -> a));
        } catch (Exception exception) {
            byCollectorJobId = Map.of();
        }

        List<CollectionJobHealthResponse> result = new ArrayList<>();
        for (CollectionTask task : collectionTaskRepository.findAll(null, null, null)) {
            if (!task.isActive() || !isCollectorTask(task)) {
                continue;
            }
            for (CollectionTaskGroup group : task.getGroups()) {
                if (!group.isActive()) {
                    continue;
                }
                result.add(toJobHealth(task, group, byCollectorJobId));
            }
        }
        return result;
    }

    private CollectionJobHealthResponse toJobHealth(
            CollectionTask task, CollectionTaskGroup group, Map<String, CollectorJobResponse> byCollectorJobId
    ) {
        Integer modelId = task.getDeviceModel() == null ? null : task.getDeviceModel().getId();
        String modelName = task.getDeviceModel() == null ? null : task.getDeviceModel().getName();
        String protocol = task.getScriptType() == null ? null : task.getScriptType().getCode();

        String collectorJobId = group.getCollectorJobId();
        if (collectorJobId == null || collectorJobId.isBlank()) {
            return new CollectionJobHealthResponse(
                    task.getId(), task.getName(), group.getId(), group.getName(), modelId, modelName, protocol,
                    null, false, 0, "NOT_SYNCED", null, null, 0, null);
        }
        CollectorJobResponse job = byCollectorJobId.get(collectorJobId);
        if (job == null) {
            return new CollectionJobHealthResponse(
                    task.getId(), task.getName(), group.getId(), group.getName(), modelId, modelName, protocol,
                    collectorJobId, false, 0, "UNKNOWN", null, null, 0, null);
        }
        String status;
        if (job.consecutiveFailureCount() > 0) {
            status = "FAILING";
        } else {
            // lastFailureAt은 과거 이력이다. 정상 tick 이후에도 Collector에 남아 있으므로
            // 현재 상태 판정에는 연속 실패 횟수만 사용한다.
            status = "NORMAL";
        }
        return new CollectionJobHealthResponse(
                task.getId(), task.getName(), group.getId(), group.getName(), modelId, modelName, protocol,
                collectorJobId, job.enabled(), job.targetCount(), status,
                job.lastSuccessAt(), job.lastFailureAt(), job.consecutiveFailureCount(), job.lastFailureReason());
    }

    private CollectionOperationsHealthResponse.Component managerComponent() {
        return component("manager", "Manager", "UP", null,
                "운영 상태 API가 실행 중입니다.", Map.of());
    }

    private CollectionOperationsHealthResponse.Component databaseComponent() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(Math.max(1, properties.getTimeoutMillis() / 1000));
            return component("mariadb", "MariaDB", valid ? "UP" : "DOWN", null,
                    valid ? "데이터베이스 연결이 정상입니다." : "데이터베이스 연결 검증에 실패했습니다.", Map.of());
        } catch (Exception exception) {
            return component("mariadb", "MariaDB", "DOWN", null, readableMessage(exception), Map.of());
        }
    }

    private CollectionOperationsHealthResponse.Component influxComponent() {
        if (!influxProperties.isEnabled()) {
            return component("influxdb", "InfluxDB", "DISABLED", influxProperties.getUrl(),
                    "InfluxDB 조회가 비활성화되어 있습니다.", Map.of());
        }
        try {
            InfluxDBClient client = influxClientProvider.getIfAvailable();
            if (client == null) {
                return component("influxdb", "InfluxDB", "DOWN", influxProperties.getUrl(),
                        "InfluxDB 클라이언트를 만들지 못했습니다.", Map.of());
            }
            boolean reachable = client.ping();
            return component("influxdb", "InfluxDB", reachable ? "UP" : "DOWN", influxProperties.getUrl(),
                    reachable ? "시계열 데이터 저장소 연결이 정상입니다." : "InfluxDB ping 응답이 없습니다.",
                    Map.of("bucket", influxProperties.getBucket()));
        } catch (Exception exception) {
            return component("influxdb", "InfluxDB", "DOWN", influxProperties.getUrl(), readableMessage(exception), Map.of());
        }
    }

    /** MQTT 수신(InfluxDB 저장)은 Collector에 통합되어 Collector 상태 응답으로 함께 확인한다. */
    static String collectorMessage(Object ingestEnabled, Object ingestConnected) {
        if (ingestEnabled == null) {
            return "수집 Job API에 연결되었습니다.";
        }
        if (!Boolean.parseBoolean(String.valueOf(ingestEnabled))) {
            return "수집 Job API에 연결되었습니다. MQTT 수신은 꺼져 있습니다.";
        }
        return Boolean.parseBoolean(String.valueOf(ingestConnected))
                ? "수집 Job API와 MQTT 수신이 정상입니다."
                : "수집 Job API에 연결되었으나 MQTT 수신 연결이 끊겨 있습니다.";
    }

    private CollectionOperationsHealthResponse.Component mqttComponent() {
        String target = properties.getMqttHost() + ":" + properties.getMqttPort();
        if (!properties.isMqttEnabled()) {
            return component("mqtt", "MQTT Broker", "DISABLED", target,
                    "MQTT 연결 확인이 비활성화되어 있습니다.", Map.of());
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(properties.getMqttHost(), properties.getMqttPort()),
                    properties.getTimeoutMillis());
            return component("mqtt", "MQTT Broker", "UP", target,
                    "MQTT TCP 포트 연결이 정상입니다.", Map.of());
        } catch (Exception exception) {
            return component("mqtt", "MQTT Broker", "DOWN", target, readableMessage(exception), Map.of());
        }
    }

    private CollectionOperationsHealthResponse.CollectorSyncSummary syncSummary(CollectorHealthResponse collectorHealth) {
        int activeGroups = 0;
        int withJob = 0;
        int missingJob = 0;
        int missingSpec = 0;
        for (CollectionTask task : collectionTaskRepository.findAll(null, null, null)) {
            if (!task.isActive() || !isCollectorTask(task)) {
                continue;
            }
            for (CollectionTaskGroup group : task.getGroups()) {
                if (!group.isActive()) {
                    continue;
                }
                activeGroups++;
                if (group.getGeneratedSpec() == null || group.getGeneratedSpec().isBlank()) {
                    missingSpec++;
                }
                if (group.getCollectorJobId() == null || group.getCollectorJobId().isBlank()) {
                    missingJob++;
                } else {
                    withJob++;
                }
            }
        }
        return new CollectionOperationsHealthResponse.CollectorSyncSummary(
                collectorJobClient.isEnabled(),
                activeGroups,
                withJob,
                missingJob,
                missingSpec,
                collectorHealth == null ? null : collectorHealth.jobs(),
                collectorHealth == null ? null : collectorHealth.instanceId()
        );
    }

    private static boolean isCollectorTask(CollectionTask task) {
        return task.getScriptType() != null
                && CollectionGroupSpecService.isCollectorProtocol(task.getScriptType().getCode());
    }

    private static CollectionOperationsHealthResponse.Component component(
            String code, String name, String status, String target, String message, Map<String, Object> details
    ) {
        return new CollectionOperationsHealthResponse.Component(code, name, status, target, message, details);
    }


    private static String readableMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
