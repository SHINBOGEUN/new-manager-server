package net.vivans.dcim.module.lora.application;

/** 설정 저장이 커밋된 뒤 Sensor Data에 즉시 재동기화를 요청하기 위한 이벤트. */
public record LoraMqttSourceChangedEvent(String reason) {
}
