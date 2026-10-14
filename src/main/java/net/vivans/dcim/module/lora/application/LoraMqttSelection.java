package net.vivans.dcim.module.lora.application;

import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;
import net.vivans.dcim.module.mqtt.domain.model.MqttPointScope;

/** 공통 MQTT 저장소에서 기존 LoRa API에 해당하는 설정만 선택한다. */
final class LoraMqttSelection {
    private LoraMqttSelection() {}

    static boolean isLora(DeviceMqttIdentity identity) {
        return identity.getIdentityType().isLora();
    }

    static boolean isLora(DeviceModelMqttPoint point) {
        return point.getComponentScope() == MqttPointScope.DEVICE
                && LoraModelTypeValidator.LORA_SENSOR_MODEL_TYPE_CODE.equals(
                        point.getDeviceModel().getDeviceType().getCode());
    }
}
