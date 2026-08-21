package net.vivans.dcim.module.collectortask.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorStartupSyncListener {

    private final CollectorSyncService collectorSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("starting collector active group repush");
        collectorSyncService.repushActiveGroups();
    }
}
