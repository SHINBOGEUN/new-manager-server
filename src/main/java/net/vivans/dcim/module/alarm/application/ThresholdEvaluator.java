package net.vivans.dcim.module.alarm.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.alarm.domain.AlarmEvent;
import net.vivans.dcim.module.alarm.domain.ThresholdAlarmEventDetail;
import net.vivans.dcim.module.alarm.domain.ThresholdRule;
import net.vivans.dcim.module.alarm.infrastructure.AlarmEventRepository;
import net.vivans.dcim.module.alarm.infrastructure.ThresholdAlarmEventDetailRepository;
import net.vivans.dcim.module.alarm.infrastructure.ThresholdRuleRepository;
import net.vivans.dcim.module.device.application.DeviceMeasurementSourceCatalog;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.SeriesPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThresholdEvaluator {
    private final ThresholdRuleRepository rules;
    private final AlarmEventRepository events;
    private final ThresholdAlarmEventDetailRepository details;
    private final PointQuery pointQuery;
    private final DeviceMeasurementSourceCatalog sourceCatalog;
    private final ApplicationEventPublisher publisher;

    private static final Duration MAX_SAMPLE_AGE = Duration.ofMinutes(5);

    @Transactional
    public void evaluate(long ruleId, Instant now) {
        ThresholdRule rule = rules.findLockedById(ruleId).orElse(null);
        if (rule == null || !rule.isEnabled()) return;
        int deviceId = rule.getDevice().getId();
        DeviceMeasurementSourceCatalog.Source source = sourceCatalog.availableSources(java.util.Set.of(deviceId))
                .stream().filter(item -> item.protocol().equalsIgnoreCase(rule.getProtocolType().getCode())
                        && item.pointName().equals(rule.getPointName()))
                .findFirst().orElse(null);
        if (source == null || !source.thresholdSupported() || source.unit() == null || source.unit().isBlank()) {
            events.findOpenThresholdEvents(rule.getId())
                    .forEach(event -> event.clear(now));
            return;
        }

        SeriesPoint latest = pointQuery.findLatestRaw(java.util.List.of(deviceId),
                        java.util.List.of(rule.getPointName()), now.plusMillis(1), rule.getProtocolType().getCode())
                .stream().filter(point -> point.deviceId() != null && point.deviceId() == deviceId
                        && point.value() != null && Double.isFinite(point.value())
                        && (point.componentType() == null || point.componentType().isBlank()))
                .findFirst().orElse(null);
        // Missing or invalid samples are unknown: retain the current event state.
        if (latest == null || !fresh(latest.time(), now)) return;

        apply(rule, source, BigDecimal.valueOf(latest.value()), now);
    }

    @Transactional
    public void evaluateSample(int deviceId, String protocol, String pointName, double value, Instant measuredAt) {
        if (!Double.isFinite(value) || !fresh(measuredAt, Instant.now())) return;
        ThresholdRule matching = rules.findFirstByDevice_IdAndPointNameAndEnabledTrue(deviceId, pointName).orElse(null);
        if (matching == null) return;
        ThresholdRule rule = rules.findLockedById(matching.getId()).orElse(null);
        if (rule == null || !rule.isEnabled() || !rule.getProtocolType().getCode().equalsIgnoreCase(protocol)
                || rule.getDevice().getId() != deviceId || !rule.getPointName().equals(pointName)) return;
        DeviceMeasurementSourceCatalog.Source source = sourceCatalog.availableSources(java.util.Set.of(deviceId))
                .stream().filter(item -> item.protocol().equalsIgnoreCase(protocol)
                        && item.pointName().equals(pointName)).findFirst().orElse(null);
        if (source == null || !source.thresholdSupported() || source.unit() == null || source.unit().isBlank()) return;
        apply(rule, source, BigDecimal.valueOf(value), Instant.now());
    }

    private static boolean fresh(Instant measuredAt, Instant now) {
        return measuredAt != null && !measuredAt.isBefore(now.minus(MAX_SAMPLE_AGE))
                && !measuredAt.isAfter(now.plusSeconds(30));
    }

    private void apply(ThresholdRule rule, DeviceMeasurementSourceCatalog.Source source,
                       BigDecimal measured, Instant now) {
        int deviceId = rule.getDevice().getId();
        boolean breached = "ABOVE".equals(rule.getOperator())
                ? measured.compareTo(rule.getThresholdValue()) > 0
                : measured.compareTo(rule.getThresholdValue()) < 0;
        var open = events.findOpenThresholdEvent(rule.getId(), deviceId);
        if (breached && open.isEmpty()) {
            AlarmEvent event = events.save(new AlarmEvent("THRESHOLD", rule.getSeverity(), "USER_VISIBLE", now));
            details.save(new ThresholdAlarmEventDetail(event, rule, rule.getDevice(), rule.getPointName(),
                    source.unit(), rule.getOperator(), rule.getThresholdValue(), measured));
            publisher.publishEvent(new AlarmCreated(event.getId()));
        } else if (!breached) {
            open.ifPresent(event -> event.clear(now));
        }
    }
}
