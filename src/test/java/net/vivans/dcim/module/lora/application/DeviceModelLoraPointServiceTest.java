package net.vivans.dcim.module.lora.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.lora.api.dto.DeviceModelLoraPointRequest;
import net.vivans.dcim.module.lora.api.dto.DeviceModelLoraPointResponse;
import net.vivans.dcim.module.lora.domain.model.DeviceModelLoraPoint;
import net.vivans.dcim.module.lora.domain.repository.DeviceModelLoraPointRepository;
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
 * LoRa 모델별 payload 필드 매핑 등록/수정은 대상 모델이 MODEL_TYPE=LORA_SENSOR일 때만
 * 허용되어야 한다. PDU 등 다른 MODEL_TYPE 모델은 등록·수정 모두 차단된다.
 */
class DeviceModelLoraPointServiceTest {

    private final DeviceModelLoraPointRepository deviceModelLoraPointRepository = mock(DeviceModelLoraPointRepository.class);
    private final DeviceModelRepository deviceModelRepository = mock(DeviceModelRepository.class);
    private final CommonCodeRepository commonCodeRepository = mock(CommonCodeRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final DeviceModelLoraPointService service = new DeviceModelLoraPointService(
            deviceModelLoraPointRepository, deviceModelRepository, commonCodeRepository,
            new LoraValueMapValidator(new ObjectMapper()), new LoraModelTypeValidator(), eventPublisher);

    @Test
    void create_withLoraSensorModel_succeeds() {
        DeviceModel model = deviceModel("LORA_SENSOR", "LoRa 센서");
        when(deviceModelRepository.findById(1)).thenReturn(Optional.of(model));
        when(commonCodeRepository.findByCodeGroupGroupKeyAndCode("DATA_POINT_TYPE", "UNCLASSIFIED"))
                .thenReturn(Optional.of(dataPointType()));
        when(deviceModelLoraPointRepository.existsByDeviceModelIdAndPayloadField(anyInt(), anyString())).thenReturn(false);
        when(deviceModelLoraPointRepository.existsByDeviceModelIdAndPointName(anyInt(), anyString())).thenReturn(false);
        when(deviceModelLoraPointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeviceModelLoraPointResponse response = service.create(1,
                new DeviceModelLoraPointRequest("object.TempC_SHT", "TEMPERATURE", null, "C", 1.0, null, true));

        assertThat(response.payloadField()).isEqualTo("object.TempC_SHT");
        assertThat(response.pointName()).isEqualTo("TEMPERATURE");
    }

    @Test
    void create_withNonLoraSensorModel_throwsAndDoesNotSave() {
        DeviceModel model = deviceModel("PDU", "PDU");
        when(deviceModelRepository.findById(1)).thenReturn(Optional.of(model));

        assertThatThrownBy(() -> service.create(1,
                new DeviceModelLoraPointRequest("object.TempC_SHT", "TEMPERATURE", null, "C", 1.0, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LoRa 설정은 MODEL_TYPE=LORA_SENSOR 모델에만 등록할 수 있습니다.");

        verify(deviceModelLoraPointRepository, never()).save(any());
    }

    @Test
    void update_withNonLoraSensorModel_throwsAndDoesNotSave() {
        DeviceModel model = deviceModel("COOLER", "쿨러");
        DeviceModelLoraPoint point = DeviceModelLoraPoint.create(
                model, "object.TempC_SHT", "TEMPERATURE", dataPointType(), "C", 1.0, null, true);
        when(deviceModelLoraPointRepository.findById(9)).thenReturn(Optional.of(point));

        assertThatThrownBy(() -> service.update(1, 9,
                new DeviceModelLoraPointRequest("object.TempC_SHT", "TEMPERATURE", null, "C", 1.0, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LoRa 설정은 MODEL_TYPE=LORA_SENSOR 모델에만 등록할 수 있습니다.");

        verify(deviceModelLoraPointRepository, never()).save(any());
    }

    private DeviceModel deviceModel(String code, String name) {
        CodeGroup group = CodeGroup.createCodeGroup("MODEL_TYPE", "모델 유형");
        CommonCode deviceType = CommonCode.createCommonCode(group, code, name, 1);
        DeviceModel model = DeviceModel.create(code + "-model", "Dragino", deviceType, null);
        setId(model, 1);
        return model;
    }

    private CommonCode dataPointType() {
        CodeGroup group = CodeGroup.createCodeGroup("DATA_POINT_TYPE", "데이터 유형");
        return CommonCode.createCommonCode(group, "TEMPERATURE", "온도", 1);
    }

    private void setId(DeviceModel model, Integer id) {
        try {
            java.lang.reflect.Field field = model.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(model, id);
        } catch (ReflectiveOperationException e) {
            // id 미설정이어도 findPoint()의 getDeviceModel().getId() 비교 외에는 테스트에 영향이 없다.
        }
    }
}
