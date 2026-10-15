package net.vivans.dcim.module.mqtt.domain.model;

/** DEVICE는 payload 루트, COMPONENT는 반복 구성요소 한 항목을 값 경로의 기준으로 사용한다. */
public enum MqttPointScope {
    DEVICE,
    COMPONENT
}
