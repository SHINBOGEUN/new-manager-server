package net.vivans.dcim.module.lora.api.dto;

import net.vivans.dcim.module.lora.domain.model.LoraIdType;

import java.time.Instant;

/**
 * 등록된 LoRa 장비(device_lora_endpoint) 1건에 대한 실제 수집 검증 결과 한 줄.
 * status 값(우선순위 순): DISABLED, NO_MAPPING, NO_SOURCE, SOURCE_DOWN,
 * INFLUX_WRITE_FAILED, FIELD_ERROR, NEVER_SAVED, SAVED
 */
public record LoraCollectionStatusRow(
        Integer endpointId,
        Integer deviceId,
        String deviceName,
        Integer deviceModelId,
        String deviceModelName,
        LoraIdType idType,
        String externalId,
        boolean endpointEnabled,
        String status,
        String statusMessage,
        Instant lastMessageSavedAt,
        Integer lastPointCount,
        String recentErrorReason,
        Instant recentErrorAt
) {
}
