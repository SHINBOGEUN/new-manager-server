package net.vivans.dcim.module.collectortask.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorHealthResponse;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorJobClient;
import net.vivans.dcim.module.pue.application.PueCollectorSyncService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorStartupSyncListener {

    private final CollectorSyncService collectorSyncService;
    private final PueCollectorSyncService pueCollectorSyncService;
    private final CollectorJobClient collectorJobClient;
    private volatile String collectorInstanceId;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        reconcile();
    }

    @Scheduled(fixedDelayString = "${collector.reconciliation.fixed-delay-ms:60000}")
    public void reconcile() {
        if (!collectorJobClient.isEnabled()) {
            return;
        }
        final CollectorHealthResponse health;
        try {
            health = collectorJobClient.health();
        } catch (Exception exception) {
            log.debug("collector unavailable; waiting to reconcile jobs: {}", exception.getMessage());
            return;
        }
        if (health.instanceId().equals(collectorInstanceId)) {
            return;
        }
        collectorInstanceId = health.instanceId();
        log.info("collector instance changed; repushing active collection jobs: instanceId={}", collectorInstanceId);
        collectorSyncService.repushActiveGroups();
        pueCollectorSyncService.repushActiveDefinitions();
    }
}
