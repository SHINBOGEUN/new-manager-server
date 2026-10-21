package net.vivans.dcim.module.alarm.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alarms.collection-gap.enabled", havingValue = "true", matchIfMissing = true)
public class CollectionGapSchedule {
    private final CollectionGapEvaluator evaluator;

    @Scheduled(fixedDelayString = "${alarms.collection-gap.interval-ms:60000}",
            initialDelayString = "${alarms.collection-gap.initial-delay-ms:60000}")
    public void tick() {
        try {
            evaluator.evaluate(Instant.now(), Duration.ofHours(1));
        } catch (RuntimeException exception) {
            log.warn("Collection gap evaluation skipped: {}", exception.getMessage(), exception);
        }
    }
}
