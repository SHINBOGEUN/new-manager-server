package net.vivans.dcim.module.alarm.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.alarm.infrastructure.ThresholdRuleRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alarms.thresholds.enabled", havingValue = "true", matchIfMissing = true)
public class ThresholdSchedule {
    private final ThresholdRuleRepository rules;
    private final ThresholdEvaluator evaluator;

    @Scheduled(fixedDelayString = "${alarms.thresholds.interval-ms:60000}", initialDelayString = "${alarms.thresholds.initial-delay-ms:60000}")
    public void tick() {
        Instant now = Instant.now();
        try {
            for (var rule : rules.findAllByEnabledTrueOrderByIdAsc()) {
                try { evaluator.evaluate(rule.getId(), now); }
                catch (RuntimeException e) { log.warn("Threshold rule {} evaluation failed: {}", rule.getId(), e.getMessage(), e); }
            }
        } catch (RuntimeException e) { log.warn("Threshold schedule skipped: {}", e.getMessage(), e); }
    }
}
