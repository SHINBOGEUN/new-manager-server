package net.vivans.dcim.module.live.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorJobClient;
import net.vivans.dcim.module.live.infrastructure.collector.LiveCollectionSpec;
import net.vivans.dcim.shared.exception.CollectorSyncException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveTelemetryCollectorSyncService {

    private final CollectorJobClient collectorJobClient;
    private final ObjectMapper objectMapper;

    public void upsert(LiveCollectionSpec spec) {
        if (!collectorJobClient.isEnabled()) {
            return;
        }
        try {
            collectorJobClient.upsertLive(objectMapper.writeValueAsString(spec));
            log.info("live collector job upserted targetCount={}", spec.targets() == null ? 0 : spec.targets().size());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize live collection spec", exception);
        } catch (Exception exception) {
            collectorJobClient.logFailure("upsertLive", null, null, exception);
            if (collectorJobClient.isFailFast()) {
                throw new CollectorSyncException("collector live sync failed: upsertLive", exception);
            }
        }
    }

    public void stop() {
        if (!collectorJobClient.isEnabled()) {
            return;
        }
        try {
            collectorJobClient.deleteLive();
            log.info("live collector job deleted");
        } catch (Exception exception) {
            collectorJobClient.logFailure("deleteLive", null, null, exception);
            if (collectorJobClient.isFailFast()) {
                throw new CollectorSyncException("collector live sync failed: deleteLive", exception);
            }
        }
    }

    public void stopQuietly() {
        if (!collectorJobClient.isEnabled()) {
            return;
        }
        try {
            collectorJobClient.deleteLive();
            log.info("live collector job deleted");
        } catch (Exception exception) {
            collectorJobClient.logFailure("deleteLive", null, null, exception);
        }
    }
}
