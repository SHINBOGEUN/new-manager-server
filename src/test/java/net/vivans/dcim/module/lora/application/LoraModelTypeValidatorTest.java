package net.vivans.dcim.module.lora.application;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LoRa 설정(엔드포인트 · 모델 매핑 · 장비 override)은 MODEL_TYPE=LORA_SENSOR 모델/장비에만
 * 등록할 수 있다는 규칙을 고정한다. CommonCode의 PK(id)가 아니라 code 값("LORA_SENSOR")으로
 * 판별하므로, id가 다른 환경(예: 시드 순서가 달라 LORA_SENSOR의 id가 52가 아닌 DB)에서도
 * 동일하게 동작해야 한다.
 */
class LoraModelTypeValidatorTest {

    private final LoraModelTypeValidator validator = new LoraModelTypeValidator();

    @Test
    void requireLoraSensor_withLoraSensorModel_doesNotThrow() {
        DeviceModel model = deviceModel("LORA_SENSOR", "LoRa 센서");

        assertThatCode(() -> validator.requireLoraSensor(model)).doesNotThrowAnyException();
    }

    @Test
    void requireLoraSensor_withPduModelType_throwsWithHumanReadableMessage() {
        DeviceModel model = deviceModel("PDU", "PDU");

        assertThatThrownBy(() -> validator.requireLoraSensor(model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LoRa 설정은 MODEL_TYPE=LORA_SENSOR 모델에만 등록할 수 있습니다.");
    }

    @Test
    void requireLoraSensor_withCoolerModelType_throws() {
        DeviceModel model = deviceModel("COOLER", "쿨러");

        assertThatThrownBy(() -> validator.requireLoraSensor(model))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireLoraSensor_withNullDeviceModel_throws() {
        assertThatThrownBy(() -> validator.requireLoraSensor((DeviceModel) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireLoraSensor_withDeviceOfLoraSensorModel_doesNotThrow() {
        DeviceModel model = deviceModel("LORA_SENSOR", "LoRa 센서");
        Device device = Device.create(model, unassignedLocation(), "센서-01", null);

        assertThatCode(() -> validator.requireLoraSensor(device)).doesNotThrowAnyException();
    }

    @Test
    void requireLoraSensor_withDeviceOfOtherModel_throws() {
        DeviceModel model = deviceModel("PDU", "PDU");
        Device device = Device.create(model, unassignedLocation(), "PDU-01", null);

        assertThatThrownBy(() -> validator.requireLoraSensor(device))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LoRa 설정은 MODEL_TYPE=LORA_SENSOR 모델에만 등록할 수 있습니다.");
    }

    @Test
    void requireLoraSensor_withNullDevice_throws() {
        assertThatThrownBy(() -> validator.requireLoraSensor((Device) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private DeviceModel deviceModel(String code, String name) {
        CodeGroup group = CodeGroup.createCodeGroup("MODEL_TYPE", "모델 유형");
        CommonCode deviceType = CommonCode.createCommonCode(group, code, name, 1);
        return DeviceModel.create(code + "-model", "Dragino", deviceType, null);
    }

    private LocationNode unassignedLocation() {
        CodeGroup group = CodeGroup.createCodeGroup("LOCATION_TYPE", "위치 유형");
        CommonCode type = CommonCode.createCommonCode(group, "UNASSIGNED", "미배정", -1);
        return LocationNode.createRoot(Device.UNASSIGNED_LOCATION_CODE, type, "미배정");
    }
}
