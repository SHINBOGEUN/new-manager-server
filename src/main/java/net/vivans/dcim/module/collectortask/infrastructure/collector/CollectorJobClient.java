package net.vivans.dcim.module.collectortask.infrastructure.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class CollectorJobClient {

    private static final ParameterizedTypeReference<CollectorApiResponse<CollectorJobResponse>> JOB_RESPONSE_TYPE =
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
        return requireData(restClient.post()
                .uri("/api/jobs/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(specJson)
                .retrieve()
                .body(JOB_RESPONSE_TYPE), "register");
    }

    public CollectorJobResponse update(String collectorJobId, String specJson) {
        return requireData(restClient.put()
                .uri("/api/jobs/{collectorJobId}", collectorJobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(specJson)
                .retrieve()
                .body(JOB_RESPONSE_TYPE), "update");
    }

    public void delete(String collectorJobId) {
        restClient.delete()
                .uri("/api/jobs/{collectorJobId}", collectorJobId)
                .retrieve()
                .toBodilessEntity();
    }

    public CollectorJobResponse toggle(String collectorJobId, boolean enabled) {
        return requireData(restClient.patch()
                .uri("/api/jobs/{collectorJobId}/toggle", collectorJobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"enabled\":" + enabled + "}")
                .retrieve()
                .body(JOB_RESPONSE_TYPE), "toggle");
    }

    private CollectorJobResponse requireData(CollectorApiResponse<CollectorJobResponse> response, String operation) {
        if (response == null || response.data() == null) {
            throw new IllegalStateException("collector " + operation + " returned empty response");
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
