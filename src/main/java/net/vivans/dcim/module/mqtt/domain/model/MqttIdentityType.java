package net.vivans.dcim.module.mqtt.domain.model;

/** MQTT 메시지를 내부 장비에 연결할 때 사용하는 외부 식별자 종류. */
public enum MqttIdentityType {
    MAC,
    DEV_EUI,
    DEVICE_NAME,
    UUID,
    SERIAL;

    public boolean isLora() {
        return this == DEV_EUI || this == DEVICE_NAME;
    }
}
