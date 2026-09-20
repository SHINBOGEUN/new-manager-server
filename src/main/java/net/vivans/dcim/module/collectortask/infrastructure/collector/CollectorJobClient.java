package net.vivans.dcim.module.collectortask.infrastructure.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

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

    public void upsertPue(Integer definitionId, String specJson) {
        withRetryVoid(() -> restClient.put().uri("/api/pue-jobs/{id}", definitionId)
                .contentType(MediaType.APPLICATION_JSON).body(specJson).retrieve().toBodilessEntity());
    }

    public void deletePue(Integer definitionId) {
        withRetryVoid(() -> restClient.delete().uri("/api/pue-jobs/{id}", definitionId).retrieve().toBodilessEntity());
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
