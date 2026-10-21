package net.vivans.dcim.module.alarm.application;

import net.vivans.dcim.module.alarm.domain.AlarmEvent;
import net.vivans.dcim.module.alarm.domain.ThresholdRule;
import net.vivans.dcim.module.alarm.infrastructure.AlarmEventRepository;
import net.vivans.dcim.module.alarm.infrastructure.ThresholdAlarmEventDetailRepository;
import net.vivans.dcim.module.alarm.infrastructure.ThresholdRuleRepository;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.application.DeviceMeasurementSourceCatalog;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ThresholdEvaluatorRealtimeTest {
    @Test
    void createsOnlyOneAlarmForRepeatedBreachingSamples() {
        var rules = mock(ThresholdRuleRepository.class);
        var events = mock(AlarmEventRepository.class);
        var details = mock(ThresholdAlarmEventDetailRepository.class);
        var catalog = mock(DeviceMeasurementSourceCatalog.class);
        var publisher = mock(ApplicationEventPublisher.class);
        var evaluator = new ThresholdEvaluator(rules, events, details, mock(PointQuery.class), catalog, publisher);
        var rule = mock(ThresholdRule.class);
        var device = mock(Device.class);
        var protocol = mock(CommonCode.class);
        when(rule.getId()).thenReturn(12L);
        when(rule.isEnabled()).thenReturn(true);
        when(rule.getDevice()).thenReturn(device);
        when(device.getId()).thenReturn(34);
        when(rule.getProtocolType()).thenReturn(protocol);
        when(protocol.getCode()).thenReturn("mqtt");
        when(rule.getPointName()).thenReturn("TEMP");
        when(rule.getOperator()).thenReturn("ABOVE");
        when(rule.getThresholdValue()).thenReturn(BigDecimal.valueOf(30));
        when(rule.getSeverity()).thenReturn("WARNING");
        when(rules.findFirstByDevice_IdAndPointNameAndEnabledTrue(34, "TEMP")).thenReturn(Optional.of(rule));
        when(rules.findLockedById(12L)).thenReturn(Optional.of(rule));
        when(catalog.availableSources(Set.of(34))).thenReturn(List.of(new DeviceMeasurementSourceCatalog.Source(
                34, "Sensor", 1, "mqtt", "TEMP", "℃", "TEMPERATURE", 34, "MODEL_POINT", false, 1, true)));
        when(events.findOpenThresholdEvent(12L, 34))
                .thenReturn(Optional.empty()).thenAnswer(invocation -> Optional.of(mock(AlarmEvent.class)));
        when(events.save(any(AlarmEvent.class))).thenAnswer(invocation -> {
            AlarmEvent event = invocation.getArgument(0);
            ReflectionTestUtils.setField(event, "id", 77L);
            return event;
        });

        evaluator.evaluateSample(34, "mqtt", "TEMP", 32, Instant.now());
        evaluator.evaluateSample(34, "mqtt", "TEMP", 33, Instant.now());

        verify(events, times(1)).save(any(AlarmEvent.class));
        verify(details, times(1)).save(any());
        verify(publisher, times(1)).publishEvent(new AlarmCreated(77));
    }

    @Test
    void ignoresStaleSamplesBeforeRuleLookup() {
        var rules = mock(ThresholdRuleRepository.class);
        var evaluator = new ThresholdEvaluator(rules, mock(AlarmEventRepository.class),
                mock(ThresholdAlarmEventDetailRepository.class), mock(PointQuery.class),
                mock(DeviceMeasurementSourceCatalog.class), mock(ApplicationEventPublisher.class));
        evaluator.evaluateSample(34, "mqtt", "TEMP", 32, Instant.now().minusSeconds(360));
        verifyNoInteractions(rules);
    }
}
