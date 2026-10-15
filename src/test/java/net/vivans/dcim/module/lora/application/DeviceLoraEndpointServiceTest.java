package net.vivans.dcim.module.lora.application;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointRequest;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointResponse;
import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LoRa 엔드포인트 등록/수정은 대상 장비의 모델이 MODEL_TYPE=LORA_SENSOR일 때만 허용되어야 한다.
 * PDU 등 다른 MODEL_TYPE 장비는 등록·수정 모두 차단되고, 이 경우 repository.save()가 호출되지
 * 않는지(즉 실제로 저장을 막는지)까지 확인한다.
 */
class DeviceLoraEndpointServiceTest {

    private final DeviceMqttIdentityRepository mqttIdentityRepository = mock(DeviceMqttIdentityRepository.class);
    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final DeviceLoraEndpointService service = new DeviceLoraEndpointService(
            mqttIdentityRepository, deviceRepository, new LoraModelTypeValidator(), eventPublisher);

    @Test
    void create_withLoraSensorDevice_succeeds() {
        Device device = device("LORA_SENSOR", "LoRa 센서", "센서-01");
        when(deviceRepository.findById(1)).thenReturn(Optional.of(device));
        when(mqttIdentityRepository.existsByDeviceIdAndIdentityType(1, MqttIdentityType.DEV_EUI)).thenReturn(false);
        when(mqttIdentityRepository.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(any(), any()))
                .thenReturn(Optional.empty());
        when(mqttIdentityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeviceLoraEndpointResponse response = service.create(
                new DeviceLoraEndpointRequest(1, MqttIdentityType.DEV_EUI, "24E124710C123456", true));

        assertThat(response.externalId()).isEqualTo("24E124710C123456");
        assertThat(response.idType()).isEqualTo(MqttIdentityType.DEV_EUI);
    }

    @Test
    void create_withNonLoraSensorDevice_throwsAndDoesNotSave() {
        Device device = device("PDU", "PDU", "PDU-01");
        when(deviceRepository.findById(1)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.create(
                new DeviceLoraEndpointRequest(1, MqttIdentityType.DEV_EUI, "24E124710C123456", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LoRa 설정은 MODEL_TYPE=LORA_SENSOR 모델에만 등록할 수 있습니다.");

        verify(mqttIdentityRepository, never()).save(any());
    }

    @Test
    void update_withNonLoraSensorDevice_throwsAndDoesNotSave() {
        Device device = device("COOLER", "쿨러", "쿨러-01");
        DeviceMqttIdentity endpoint = DeviceMqttIdentity.create(device, MqttIdentityType.DEV_EUI, "24E124710C123456", true);
        when(mqttIdentityRepository.findById(5)).thenReturn(Optional.of(endpoint));

        assertThatThrownBy(() -> service.update(5,
                new DeviceLoraEndpointRequest(1, MqttIdentityType.DEV_EUI, "24E124710C999999", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LoRa 설정은 MODEL_TYPE=LORA_SENSOR 모델에만 등록할 수 있습니다.");

        verify(mqttIdentityRepository, never()).save(any());
    }

    @Test
    void update_withLoraSensorDevice_succeeds() {
        Device device = device("LORA_SENSOR", "LoRa 센서", "센서-02");
        DeviceMqttIdentity endpoint = DeviceMqttIdentity.create(device, MqttIdentityType.DEV_EUI, "24E124710C123456", true);
        when(mqttIdentityRepository.findById(5)).thenReturn(Optional.of(endpoint));
        when(mqttIdentityRepository.existsByDeviceIdAndIdentityTypeAndIdNot(anyInt(), any(), anyInt())).thenReturn(false);
        when(mqttIdentityRepository.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(any(), any()))
                .thenReturn(Optional.empty());
        when(mqttIdentityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeviceLoraEndpointResponse response = service.update(5,
                new DeviceLoraEndpointRequest(1, MqttIdentityType.DEV_EUI, "24E124710C999999", true));

        assertThat(response.externalId()).isEqualTo("24E124710C999999");
    }

    private Device device(String modelTypeCode, String modelTypeName, String deviceName) {
        CodeGroup modelTypeGroup = CodeGroup.createCodeGroup("MODEL_TYPE", "모델 유형");
        CommonCode deviceType = CommonCode.createCommonCode(modelTypeGroup, modelTypeCode, modelTypeName, 1);
        DeviceModel model = DeviceModel.create(modelTypeCode + "-model", "Dragino", deviceType, null);
        Device device = Device.create(model, unassignedLocation(), deviceName, null);
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", 1);
        return device;
    }

    private LocationNode unassignedLocation() {
        CodeGroup group = CodeGroup.createCodeGroup("LOCATION_TYPE", "위치 유형");
        CommonCode type = CommonCode.createCommonCode(group, "UNASSIGNED", "미배정", -1);
        return LocationNode.createRoot(Device.UNASSIGNED_LOCATION_CODE, type, "미배정");
    }
}
