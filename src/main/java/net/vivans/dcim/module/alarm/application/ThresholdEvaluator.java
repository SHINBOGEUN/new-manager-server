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

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThresholdEvaluator {
    private final ThresholdRuleRepository rules;
    private final AlarmEventRepository events;
    private final ThresholdAlarmEventDetailRepository details;
    private final PointQuery pointQuery;
    private final DeviceMeasurementSourceCatalog sourceCatalog;

    @Transactional
    public void evaluate(long ruleId, Instant now) {
        ThresholdRule rule = rules.findById(ruleId).orElse(null);
        if (rule == null || !rule.isEnabled()) return;
        int deviceId = rule.getDevice().getId();
        DeviceMeasurementSourceCatalog.Source source = sourceCatalog.availableSources(java.util.Set.of(deviceId))
                .stream().filter(item -> item.protocol().equalsIgnoreCase(rule.getProtocolType().getCode())
                        && item.pointName().equals(rule.getPointName()))
                .findFirst().orElse(null);
        if (source == null || !source.thresholdSupported() || source.unit() == null || source.unit().isBlank()) {
            events.findAllBySourceKeyAndClearedDtIsNull("threshold:" + rule.getId())
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
        if (latest == null) return;

        BigDecimal measured = BigDecimal.valueOf(latest.value());
        boolean breached = "ABOVE".equals(rule.getOperator())
                ? measured.compareTo(rule.getThresholdValue()) > 0
                : measured.compareTo(rule.getThresholdValue()) < 0;
        String sourceKey = "threshold:" + rule.getId();
        String subjectKey = "device:" + deviceId;
        var open = events.findFirstBySourceKeyAndSubjectKeyAndClearedDtIsNull(sourceKey, subjectKey);
        if (breached && open.isEmpty()) {
            AlarmEvent event = events.save(new AlarmEvent("THRESHOLD", rule.getSeverity(), "USER_VISIBLE",
                    sourceKey, subjectKey, now));
            details.save(new ThresholdAlarmEventDetail(event, rule, rule.getDevice(), rule.getPointName(),
                    source.unit(), rule.getOperator(), rule.getThresholdValue(), measured));
        } else if (!breached) {
            open.ifPresent(event -> event.clear(now));
        }
    }
}
