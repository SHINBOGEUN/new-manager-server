package net.vivans.dcim.module.live.application;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.live.api.dto.LiveSelectionItemRequest;
import net.vivans.dcim.module.live.api.dto.LiveSelectionItemResponse;
import net.vivans.dcim.module.live.api.dto.LiveSelectionRequest;
import net.vivans.dcim.module.live.api.dto.LiveSelectionResponse;
import net.vivans.dcim.module.live.config.LiveTelemetryProperties;
import net.vivans.dcim.module.live.infrastructure.collector.LiveCollectionSpec;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class LiveTelemetrySelectionService {

    private final LiveTelemetryQueryService liveTelemetryQueryService;
    private final LiveTelemetrySpecService liveTelemetrySpecService;
    private final LiveTelemetryCollectorSyncService liveTelemetryCollectorSyncService;
    private final LiveTelemetryProperties properties;
    private final ScheduledExecutorService scheduler;
    private final AtomicReference<LiveSelectionResponse> current =
            new AtomicReference<>(emptySelection());
    private ScheduledFuture<?> sessionEndTask;

    public LiveTelemetrySelectionService(
            LiveTelemetryQueryService liveTelemetryQueryService,
            LiveTelemetrySpecService liveTelemetrySpecService,
            LiveTelemetryCollectorSyncService liveTelemetryCollectorSyncService,
            LiveTelemetryProperties properties
    ) {
        this.liveTelemetryQueryService = liveTelemetryQueryService;
        this.liveTelemetrySpecService = liveTelemetrySpecService;
        this.liveTelemetryCollectorSyncService = liveTelemetryCollectorSyncService;
        this.properties = properties;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "live-telemetry-session");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized LiveSelectionResponse getSelection() {
        return copyOf(current.get());
    }

    public LiveSelectionResponse updateSelection(LiveSelectionRequest request) {
        List<LiveSelectionItemRequest> items = liveTelemetryQueryService.normalizeAndValidate(
                request == null ? List.of() : request.items()
        );
        if (items.isEmpty()) {
            liveTelemetryCollectorSyncService.stop();
            return clearLocalSelection();
        }
        LiveCollectionSpec spec = liveTelemetrySpecService.build(items);
        liveTelemetryCollectorSyncService.upsert(spec);
        return storeSelection(items);
    }

    public LiveSelectionResponse clearSelection() {
        liveTelemetryCollectorSyncService.stop();
        return clearLocalSelection();
    }

    private synchronized LiveSelectionResponse storeSelection(List<LiveSelectionItemRequest> items) {
        Instant expiresAt = resolveExpiresAt();
        LiveSelectionResponse saved = new LiveSelectionResponse(toResponses(items), expiresAt);
        current.set(saved);
        scheduleSessionEnd(expiresAt);
        return copyOf(saved);
    }

    private synchronized LiveSelectionResponse clearLocalSelection() {
        cancelSessionEnd();
        LiveSelectionResponse empty = emptySelection();
        current.set(empty);
        return empty;
    }

    private Instant resolveExpiresAt() {
        int timeoutMinutes = properties.getSessionTimeoutMinutes();
        if (timeoutMinutes <= 0) {
            return null;
        }
        return Instant.now().plusSeconds(timeoutMinutes * 60L);
    }

    private void scheduleSessionEnd(Instant expiresAt) {
        cancelSessionEnd();
        if (expiresAt == null) {
            return;
        }
        long delayMs = Math.max(0L, expiresAt.toEpochMilli() - Instant.now().toEpochMilli());
        sessionEndTask = scheduler.schedule(this::endSession, delayMs, TimeUnit.MILLISECONDS);
    }

    private void endSession() {
        liveTelemetryCollectorSyncService.stopQuietly();
        synchronized (this) {
            sessionEndTask = null;
            if (current.get().items().isEmpty()) {
                return;
            }
            log.info("live telemetry session expired");
            current.set(emptySelection());
        }
    }

    private void cancelSessionEnd() {
        if (sessionEndTask != null) {
            sessionEndTask.cancel(false);
            sessionEndTask = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        synchronized (this) {
            cancelSessionEnd();
        }
        liveTelemetryCollectorSyncService.stopQuietly();
        scheduler.shutdownNow();
    }

    private static LiveSelectionResponse emptySelection() {
        return new LiveSelectionResponse(List.of(), null);
    }

    private static List<LiveSelectionItemResponse> toResponses(List<LiveSelectionItemRequest> items) {
        List<LiveSelectionItemResponse> responses = new ArrayList<>();
        for (LiveSelectionItemRequest item : items) {
            responses.add(new LiveSelectionItemResponse(item.deviceId(), item.pointNames()));
        }
        return List.copyOf(responses);
    }

    private static LiveSelectionResponse copyOf(LiveSelectionResponse selection) {
        return new LiveSelectionResponse(List.copyOf(selection.items()), selection.expiresAt());
    }
}
