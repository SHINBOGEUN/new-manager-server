package net.vivans.dcim.module.alarm.application;

import net.vivans.dcim.module.alarm.domain.AlarmEvent;
import net.vivans.dcim.module.alarm.domain.CollectionGapAlarmDetail;
import net.vivans.dcim.module.alarm.infrastructure.AlarmEventRepository;
import net.vivans.dcim.module.alarm.infrastructure.CollectionGapAlarmDetailRepository;
import net.vivans.dcim.module.collectortask.application.CollectionGroupSpecService;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class CollectionGapEvaluatorTest {
    private final CollectionTaskRepository collectionTasks = mock(CollectionTaskRepository.class);
    private final CollectionGroupSpecService groupSpecService = mock(CollectionGroupSpecService.class);
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final DeviceMqttIdentityRepository mqttIdentities = mock(DeviceMqttIdentityRepository.class);
    private final PointQuery pointQuery = mock(PointQuery.class);
    private final AlarmEventRepository events = mock(AlarmEventRepository.class);
    private final CollectionGapAlarmDetailRepository gapDetails = mock(CollectionGapAlarmDetailRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final CollectionGapEvaluator evaluator = new CollectionGapEvaluator(collectionTasks, groupSpecService,
            devices, mqttIdentities, pointQuery, events, gapDetails, publisher);

    private final Device device = mock(Device.class);
    private final DeviceMqttIdentity identity = mock(DeviceMqttIdentity.class);
    private final Instant now = Instant.parse("2026-10-21T00:00:00Z");

    @BeforeEach
    void setUp() {
        when(collectionTasks.findAll(null, null, true)).thenReturn(List.of());
        when(mqttIdentities.findAllOrderByIdAsc()).thenReturn(List.of(identity));
        when(identity.isEnabled()).thenReturn(true);
        when(identity.getDevice()).thenReturn(device);
        when(identity.getCreatedDt()).thenReturn(now.minus(Duration.ofHours(2)));
        when(device.isEnabled()).thenReturn(true);
        when(device.getId()).thenReturn(42);
        when(gapDetails.findAllOpen()).thenReturn(List.of());
        when(pointQuery.findLastInRange(anyList(), anyList(), any(), any(), anyString())).thenReturn(List.of());
        when(events.save(any(AlarmEvent.class))).thenAnswer(invocation -> {
            AlarmEvent event = invocation.getArgument(0);
            ReflectionTestUtils.setField(event, "id", 101L);
            return event;
        });
    }

    @Test
    void raisesOneUserVisibleWarningAfterAnHourWithoutSamples() {
        evaluator.evaluate(now, Duration.ofHours(1));

        var event = org.mockito.ArgumentCaptor.forClass(AlarmEvent.class);
        verify(events).save(event.capture());
        assertThat(event.getValue().getAlarmType()).isEqualTo("COLLECTION_GAP");
        assertThat(event.getValue().getSeverity()).isEqualTo("WARNING");
        assertThat(event.getValue().getVisibility()).isEqualTo("USER_VISIBLE");
        verify(gapDetails).save(any(CollectionGapAlarmDetail.class));
        verify(publisher).publishEvent(new AlarmCreated(101L));
    }

    @Test
    void doesNotRaiseAlarmUntilTheConfiguredTargetHasBeenMonitoredForAnHour() {
        when(identity.getCreatedDt()).thenReturn(now.minus(Duration.ofMinutes(45)));

        evaluator.evaluate(now, Duration.ofHours(1));

        verify(events, never()).save(any(AlarmEvent.class));
    }

    @Test
    void clearsOpenAlarmWhenSamplesResume() {
        AlarmEvent event = new AlarmEvent("COLLECTION_GAP", "WARNING", "USER_VISIBLE", now.minus(Duration.ofHours(2)));
        CollectionGapAlarmDetail detail = new CollectionGapAlarmDetail(event, device, "mqtt");
        when(gapDetails.findAllOpen()).thenReturn(List.of(detail));
        when(pointQuery.findLastInRange(anyList(), anyList(), any(), any(), anyString()))
                .thenReturn(List.of(new LastPoint(42, "TEMP", 23.1, now.minus(Duration.ofMinutes(5)))));

        evaluator.evaluate(now, Duration.ofHours(1));

        assertThat(event.getClearedDt()).isEqualTo(now);
        verify(events, never()).save(any(AlarmEvent.class));
    }
}
