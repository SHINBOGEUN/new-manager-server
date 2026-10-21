package net.vivans.dcim.module.alarm.application;

import net.vivans.dcim.module.alarm.domain.AlarmEvent;
import net.vivans.dcim.module.alarm.domain.ThresholdAlarmEventDetail;
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
        when(rule.getWarningThresholdValue()).thenReturn(BigDecimal.valueOf(30));
        when(rule.getCriticalThresholdValue()).thenReturn(null);
        when(rules.findFirstByDevice_IdAndPointNameAndEnabledTrue(34, "TEMP")).thenReturn(Optional.of(rule));
        when(rules.findLockedById(12L)).thenReturn(Optional.of(rule));
        when(catalog.availableSources(Set.of(34))).thenReturn(List.of(new DeviceMeasurementSourceCatalog.Source(
                34, "Sensor", 1, "mqtt", "TEMP", "℃", "TEMPERATURE", 34, "MODEL_POINT", false, 1, true)));
        AlarmEvent open = mock(AlarmEvent.class);
        when(open.getSeverity()).thenReturn("WARNING");
        when(events.findOpenThresholdEvent(12L, 34))
                .thenReturn(Optional.empty()).thenAnswer(invocation -> Optional.of(open));
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
    void criticalThresholdTakesPrecedenceWhenBothLevelsAreBreached() {
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
        when(rule.getPointName()).thenReturn("POWER");
        when(rule.getOperator()).thenReturn("ABOVE");
        when(rule.getWarningThresholdValue()).thenReturn(BigDecimal.valueOf(500));
        when(rule.getCriticalThresholdValue()).thenReturn(BigDecimal.valueOf(1000));
        when(rules.findFirstByDevice_IdAndPointNameAndEnabledTrue(34, "POWER")).thenReturn(Optional.of(rule));
        when(rules.findLockedById(12L)).thenReturn(Optional.of(rule));
        when(catalog.availableSources(Set.of(34))).thenReturn(List.of(new DeviceMeasurementSourceCatalog.Source(
                34, "Meter", 1, "mqtt", "POWER", "W", "POWER", 34, "MODEL_POINT", false, 1, true)));
        when(events.findOpenThresholdEvent(12L, 34)).thenReturn(Optional.empty());
        when(events.save(any(AlarmEvent.class))).thenAnswer(invocation -> {
            AlarmEvent event = invocation.getArgument(0);
            ReflectionTestUtils.setField(event, "id", 88L);
            return event;
        });

        evaluator.evaluateSample(34, "mqtt", "POWER", 1200, Instant.now());

        var eventCaptor = org.mockito.ArgumentCaptor.forClass(AlarmEvent.class);
        verify(events).save(eventCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(eventCaptor.getValue().getSeverity()).isEqualTo("CRITICAL");
        var detailCaptor = org.mockito.ArgumentCaptor.forClass(ThresholdAlarmEventDetail.class);
        verify(details).save(detailCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(detailCaptor.getValue().getThresholdValue()).isEqualByComparingTo("1000");
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
