package net.vivans.dcim.module.mqtt.domain.model;

/** 소스별 메시지 처리 규격. AGENT는 deviceInfo/object/components 공통 에이전트 JSON이다. */
public enum MqttSourceType {
    CHIRPSTACK_LORA,
    AGENT
}
