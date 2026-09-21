package net.vivans.dcim.module.lora.application;

/** LoRa endpoint/포인트 매핑 변경 후 Sensor Data의 설정 캐시를 즉시 갱신한다. */
public record LoraConfigChangedEvent(String reason) {
}
