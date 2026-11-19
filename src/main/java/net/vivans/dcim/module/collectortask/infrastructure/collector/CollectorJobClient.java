package net.vivans.dcim.module.collectortask.infrastructure.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CollectorJobClient {

    private static final ParameterizedTypeReference<CollectorApiResponse<CollectorJobResponse>> JOB_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<CollectorApiResponse<CollectorHealthResponse>> HEALTH_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<CollectorApiResponse<List<CollectorJobResponse>>> JOB_LIST_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<CollectorApiResponse<CollectorModbusPreviewResponse>> MODBUS_PREVIEW_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<CollectorApiResponse<CollectorCalculatedJobResponse>> CALCULATED_JOB_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final CollectorServiceProperties properties;

    public CollectorJobClient(
            @Qualifier("collectorRestClient") RestClient restClient,
            CollectorServiceProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public CollectorJobResponse register(String specJson) {
        return withRetry(() -> requireData(restClient.post()
                .uri("/api/jobs/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(specJson)
                .retrieve()
                .body(JOB_RESPONSE_TYPE), "register"));
    }

    public CollectorJobResponse update(String collectorJobId, String specJson) {
        return withRetry(() -> requireData(restClient.put()
                .uri("/api/jobs/{collectorJobId}", collectorJobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(specJson)
                .retrieve()
                .body(JOB_RESPONSE_TYPE), "update"));
    }

    public void delete(String collectorJobId) {
        withRetryVoid(() -> restClient.delete()
                .uri("/api/jobs/{collectorJobId}", collectorJobId)
                .retrieve()
                .toBodilessEntity());
    }

    public CollectorJobResponse toggle(String collectorJobId, boolean enabled) {
        return withRetry(() -> requireData(restClient.patch()
                .uri("/api/jobs/{collectorJobId}/toggle", collectorJobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"enabled\":" + enabled + "}")
                .retrieve()
                .body(JOB_RESPONSE_TYPE), "toggle"));
    }

    public CollectorJobResponse upsertLive(String specJson) {
        return withRetry(() -> requireData(restClient.put()
                .uri("/api/jobs/live")
                .contentType(MediaType.APPLICATION_JSON)
                .body(specJson)
                .retrieve()
                .body(JOB_RESPONSE_TYPE), "upsertLive"));
    }

    public void deleteLive() {
        withRetryVoid(() -> restClient.delete()
                .uri("/api/jobs/live")
                .retrieve()
                .toBodilessEntity());
    }

    public void upsertCalculated(Integer definitionId, String specJson) {
        withRetryVoid(() -> restClient.put().uri("/api/calculated-jobs/{id}", definitionId)
                .contentType(MediaType.APPLICATION_JSON).body(specJson).retrieve().toBodilessEntity());
    }

    public void deleteCalculated(Integer definitionId) {
        withRetryVoid(() -> restClient.delete().uri("/api/calculated-jobs/{id}", definitionId).retrieve().toBodilessEntity());
    }

    public CollectorCalculatedJobResponse calculatedJobStatus(Integer definitionId) {
        CollectorApiResponse<CollectorCalculatedJobResponse> response = restClient.get()
                .uri("/api/calculated-jobs/{id}", definitionId).retrieve().body(CALCULATED_JOB_TYPE);
        return response == null ? null : response.data();
    }

    <T> T withRetry(java.util.function.Supplier<T> action) {
        return CollectorSyncRetryExecutor.execute(
                action,
                properties.getRetryMaxAttempts(),
                properties.getRetryDelayMs()
        );
    }

    void withRetryVoid(Runnable action) {
        CollectorSyncRetryExecutor.executeVoid(
                action,
                properties.getRetryMaxAttempts(),
                properties.getRetryDelayMs()
        );
    }

    public boolean isFailFast() {
        return properties.isFailFast();
    }

    public CollectorHealthResponse health() {
        return requireHealth(restClient.get()
                .uri("/api/health")
                .retrieve()
                .body(HEALTH_RESPONSE_TYPE));
    }

    /**
     * 현재 Collector 메모리에 올라간 job 목록(실패/복구 상태 포함)을 조회한다.
     * 운영 콘솔의 수집 실패 가시성 API가 사용하며, health()와 마찬가지로 재시도 없이 즉시
     * 결과를 반환한다(실패하면 호출부가 빈 목록 등으로 처리한다).
     */
    public List<CollectorJobResponse> list() {
        CollectorApiResponse<List<CollectorJobResponse>> response = restClient.get()
                .uri("/api/jobs")
                .retrieve()
                .body(JOB_LIST_RESPONSE_TYPE);
        return response == null || response.data() == null ? List.of() : response.data();
    }

    /** 상태 조회는 재시도하지 않는다. 구 Collector/연결 장애는 UNKNOWN으로 처리한다. */
    public List<CollectorDeviceResultResponse> deviceResults(java.util.Collection<Integer> deviceIds) {
        if(!properties.isEnabled() || deviceIds==null || deviceIds.isEmpty())return List.of();
        try {
            CollectorApiResponse<List<CollectorDeviceResultResponse>> response=restClient.get()
                .uri(builder->builder.path("/api/jobs/device-results").queryParam("deviceIds",deviceIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))).build())
                .retrieve().body(new ParameterizedTypeReference<CollectorApiResponse<List<CollectorDeviceResultResponse>>>(){});
            return response==null || response.data()==null?List.of():response.data();
        } catch(RuntimeException exception){log.debug("collector device results unavailable: {}",exception.getClass().getSimpleName());return List.of();}
    }

    /** 일회성 물리 읽기. Collector에 job을 만들거나 MQTT로 발행하지 않는다. */
    public CollectorModbusPreviewResponse previewModbus(Map<String, Object> request) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("collector service is disabled");
        }
        CollectorApiResponse<CollectorModbusPreviewResponse> response = restClient.post()
                .uri("/api/modbus/read-preview")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(MODBUS_PREVIEW_TYPE);
        if (response == null || response.data() == null) {
            throw new IllegalStateException("collector Modbus preview returned empty response");
        }
        return response.data();
    }

    private CollectorJobResponse requireData(CollectorApiResponse<CollectorJobResponse> response, String operation) {
        if (response == null || response.data() == null) {
            throw new IllegalStateException("collector " + operation + " returned empty response");
        }
        return response.data();
    }

    private CollectorHealthResponse requireHealth(CollectorApiResponse<CollectorHealthResponse> response) {
        if (response == null || response.data() == null || response.data().instanceId() == null
                || response.data().instanceId().isBlank()) {
            throw new IllegalStateException("collector health returned no instanceId");
        }
        return response.data();
    }

    public void logFailure(String operation, Integer taskId, Integer groupId, Exception exception) {
        if (exception instanceof RestClientResponseException restException) {
            log.error(
                    "collector {} failed: taskId={}, groupId={}, status={}, body={}",
                    operation,
                    taskId,
                    groupId,
                    restException.getStatusCode().value(),
                    restException.getResponseBodyAsString(),
                    exception
            );
            return;
        }
        log.error("collector {} failed: taskId={}, groupId={}", operation, taskId, groupId, exception);
    }
}
