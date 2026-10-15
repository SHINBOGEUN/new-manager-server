package net.vivans.dcim.module.mqtt.application;

import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.mqtt.domain.model.*;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import net.vivans.dcim.module.mqtt.api.dto.MqttIdentityAdminRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;

class MqttIdentityAdminServiceTest {
    final DeviceMqttIdentityRepository identities = mock(DeviceMqttIdentityRepository.class);
    final DeviceRepository devices = mock(DeviceRepository.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final MqttIdentityAdminService service = new MqttIdentityAdminService(identities, devices, events);
    final Device device = mock(Device.class);
    final DeviceModel model = mock(DeviceModel.class);
    final CommonCode type = mock(CommonCode.class);
    @BeforeEach void setup() {
        when(device.getId()).thenReturn(33);
        when(device.getDeviceModel()).thenReturn(model);
        when(model.getDeviceType()).thenReturn(type);
        when(type.getCode()).thenReturn("GPU_SERVER");
        var protocol = mock(DeviceModelProtocol.class);
        var mqtt = mock(CommonCode.class);
        when(mqtt.getCode()).thenReturn("mqtt");
        when(protocol.getProtocolType()).thenReturn(mqtt);
        when(model.getProtocols()).thenReturn(List.of(protocol));
        when(devices.findById(33)).thenReturn(Optional.of(device));
        when(identities.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void createsMac() {
        var result = service.create(new MqttIdentityAdminRequest(33, MqttIdentityType.MAC, "9c:6b:00:9b:49:71", true));
        assertThat(result.deviceId()).isEqualTo(33);
        assertThat(result.idType()).isEqualTo(MqttIdentityType.MAC);
        verify(events).publishEvent(any(MqttConfigChangedEvent.class));
    }
    @Test void macChangePreservesIdentityAndDevice() {
        var identity = DeviceMqttIdentity.create(device, MqttIdentityType.MAC, "9c:6b:00:9b:49:71", true);
        ReflectionTestUtils.setField(identity, "id", 7);
        when(identities.findById(7)).thenReturn(Optional.of(identity));
        var result = service.update(7, new MqttIdentityAdminRequest(33, MqttIdentityType.MAC, "9c:6b:00:9b:49:72", true));
        assertThat(result.id()).isEqualTo(7);
        assertThat(result.deviceId()).isEqualTo(33);
        assertThat(result.externalId()).endsWith("72");
    }
    @Test void cannotMoveIdentityToAnotherDevice() {
        var identity = DeviceMqttIdentity.create(device, MqttIdentityType.MAC, "9c:6b:00:9b:49:71", true);
        when(identities.findById(7)).thenReturn(Optional.of(identity));
        assertThatThrownBy(() -> service.update(7, new MqttIdentityAdminRequest(34, MqttIdentityType.MAC, "9c:6b:00:9b:49:72", true)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(identities, never()).save(any());
    }
    @Test void loraCannotUseMac() {
        when(type.getCode()).thenReturn("LORA_SENSOR");
        assertThatThrownBy(() -> service.create(new MqttIdentityAdminRequest(33, MqttIdentityType.MAC, "9c:6b:00:9b:49:71", true)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void inactiveOtherDevicesIdentityStillConflicts() {
        var other = DeviceMqttIdentity.create(device, MqttIdentityType.MAC, "9c:6b:00:9b:49:71", false);
        ReflectionTestUtils.setField(other, "id", 8);
        when(identities.findByIdentityTypeAndNormalizedExternalId(any(), any())).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.create(new MqttIdentityAdminRequest(33, MqttIdentityType.MAC, "9c:6b:00:9b:49:71", true)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
