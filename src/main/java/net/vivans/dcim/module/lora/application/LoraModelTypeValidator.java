package net.vivans.dcim.module.lora.application;

import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import org.springframework.stereotype.Component;

/**
 * LoRa 설정(엔드포인트 · 모델 매핑) 등록/수정 시 대상 모델·장비가
 * MODEL_TYPE=LORA_SENSOR 인지 검증한다.
 *
 * DeviceModel.deviceType은 생성 시점에 이미 MODEL_TYPE 코드그룹 소속임이 보장되므로
 * (DeviceModel#validateDeviceType), 여기서는 code 값이 LORA_SENSOR인지만 비교하면 되고
 * CommonCode의 PK(id)는 하드코딩하지 않는다.
 */
@Component
public class LoraModelTypeValidator {

    public static final String LORA_SENSOR_MODEL_TYPE_CODE = "LORA_SENSOR";

    private static final String VIOLATION_MESSAGE =
            "LoRa 설정은 MODEL_TYPE=LORA_SENSOR 모델에만 등록할 수 있습니다.";

    public void requireLoraSensor(DeviceModel deviceModel) {
        if (deviceModel == null || !LORA_SENSOR_MODEL_TYPE_CODE.equals(deviceModel.getDeviceType().getCode())) {
            throw new IllegalArgumentException(VIOLATION_MESSAGE);
        }
    }

    public void requireLoraSensor(Device device) {
        requireLoraSensor(device == null ? null : device.getDeviceModel());
    }
}
