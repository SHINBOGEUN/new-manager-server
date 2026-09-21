package net.vivans.dcim.module.lora.application;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraPointOverrideRequest;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraPointOverrideResponse;
import net.vivans.dcim.module.lora.domain.model.DeviceLoraPointOverride;
import net.vivans.dcim.module.lora.domain.repository.DeviceLoraPointOverrideRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LoRa 장비 override 등록/수정은 대상 장비의 모델이 MODEL_TYPE=LORA_SENSOR일 때만 허용되어야 한다.
 * PDU 등 다른 MODEL_TYPE 장비는 등록·수정 모두 차단된다.
 */
class DeviceLoraPointOverrideServiceTest {

    private final DeviceLoraPointOverrideRepository deviceLoraPointOverrideRepository = mock(DeviceLoraPointOverrideRepository.class);
    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final CommonCodeRepository commonCodeRepository = mock(CommonCodeRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final DeviceLoraPointOverrideService service = new DeviceLoraPointOverrideService(
            deviceLoraPointOverrideRepository, deviceRepository, commonCodeRepository,
            new LoraValueMapValidator(new com.fasterxml.jackson.databind.ObjectMapper()), new LoraModelTypeValidator(), eventPublisher);

    @Test
    void create_withLoraSensorDevice_succeeds() {
        Device device = device("LORA_SENSOR", "LoRa 센서", "센서-01");
        when(deviceRepository.findById(1)).thenReturn(Optional.of(device));
        when(commonCodeRepository.findByCodeGroupGroupKeyAndCode("DATA_POINT_TYPE", "UNCLASSIFIED"))
                .thenReturn(Optional.of(dataPointType()));
        when(deviceLoraPointOverrideRepository.existsByDeviceIdAndPayloadField(anyInt(), anyString())).thenReturn(false);
        when(deviceLoraPointOverrideRepository.existsByDeviceIdAndPointName(anyInt(), anyString())).thenReturn(false);
        when(deviceLoraPointOverrideRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeviceLoraPointOverrideResponse response = service.create(1,
                new DeviceLoraPointOverrideRequest("object.TempC_SHT", "TEMPERATURE", null, "C", 1.0, null, true));

        assertThat(response.payloadField()).isEqualTo("object.TempC_SHT");
        assertThat(response.pointName()).isEqualTo("TEMPERATURE");
    }

    @Test
    void create_withNonLoraSensorDevice_throwsAndDoesNotSave() {
        Device device = device("PDU", "PDU", "PDU-01");
        when(deviceRepository.findById(1)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.create(1,
                new DeviceLoraPointOverrideRequest("object.TempC_SHT", "TEMPERATURE", null, "C", 1.0, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LoRa 설정은 MODEL_TYPE=LORA_SENSOR 모델에만 등록할 수 있습니다.");

        verify(deviceLoraPointOverrideRepository, never()).save(any());
    }

    @Test
    void update_withNonLoraSensorDevice_throwsAndDoesNotSave() {
        Device device = device("COOLER", "쿨러", "쿨러-01");
        DeviceLoraPointOverride override = DeviceLoraPointOverride.create(
                device, "object.TempC_SHT", "TEMPERATURE", dataPointType(), "C", 1.0, null, true);
        when(deviceLoraPointOverrideRepository.findById(9)).thenReturn(Optional.of(override));

        assertThatThrownBy(() -> service.update(1, 9,
                new DeviceLoraPointOverrideRequest("object.TempC_SHT", "TEMPERATURE", null, "C", 1.0, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LoRa 설정은 MODEL_TYPE=LORA_SENSOR 모델에만 등록할 수 있습니다.");

        verify(deviceLoraPointOverrideRepository, never()).save(any());
    }

    private Device device(String modelTypeCode, String modelTypeName, String deviceName) {
        CodeGroup modelTypeGroup = CodeGroup.createCodeGroup("MODEL_TYPE", "모델 유형");
        CommonCode deviceType = CommonCode.createCommonCode(modelTypeGroup, modelTypeCode, modelTypeName, 1);
        DeviceModel model = DeviceModel.create(modelTypeCode + "-model", "Dragino", deviceType, null);
        Device device = Device.create(model, unassignedLocation(), deviceName, null);
        setId(device, 1);
        return device;
    }

    private CommonCode dataPointType() {
        CodeGroup group = CodeGroup.createCodeGroup("DATA_POINT_TYPE", "데이터 유형");
        return CommonCode.createCommonCode(group, "TEMPERATURE", "온도", 1);
    }

    private LocationNode unassignedLocation() {
        CodeGroup group = CodeGroup.createCodeGroup("LOCATION_TYPE", "위치 유형");
        CommonCode type = CommonCode.createCommonCode(group, "UNASSIGNED", "미배정", -1);
        return LocationNode.createRoot(Device.UNASSIGNED_LOCATION_CODE, type, "미배정");
    }

    private void setId(Device device, Integer id) {
        try {
            java.lang.reflect.Field field = device.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(device, id);
        } catch (ReflectiveOperationException e) {
            // id 미설정이어도 findOverride()의 getDevice().getId() 비교 외에는 테스트에 영향이 없다.
        }
    }
}
