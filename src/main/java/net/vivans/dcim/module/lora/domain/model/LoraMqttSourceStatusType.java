package net.vivans.dcim.module.lora.domain.model;

/** Sensor Data가 보고하는 MQTT 수집 소스의 현재 연결 상태. */
public enum LoraMqttSourceStatusType {
    NOT_SYNCED,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR,
    DISABLED
}
