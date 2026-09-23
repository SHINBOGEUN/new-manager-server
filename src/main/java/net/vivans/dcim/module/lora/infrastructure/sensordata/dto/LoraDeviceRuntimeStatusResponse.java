package net.vivans.dcim.module.lora.infrastructure.sensordata.dto;

import java.time.Instant;

/**
 * Sensor Data의 LoRa 런타임 상태 조회 API(GET /api/internal/lora/runtime-status) 응답 항목.
 * Sensor Data 메모리 상태이며 재시작 시 초기화된다. raw payload, MQTT 비밀번호 등은 포함하지 않는다.
 */
public record LoraDeviceRuntimeStatusResponse(
        Integer deviceId,
        Instant lastReceivedAt,
        Instant lastSavedAt,
        Integer lastPointCount,
        String lastErrorCode,
        String lastErrorMessage,
        Instant lastErrorAt
) {
}
