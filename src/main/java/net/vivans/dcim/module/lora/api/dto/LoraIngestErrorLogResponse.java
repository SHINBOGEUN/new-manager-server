package net.vivans.dcim.module.lora.api.dto;

import net.vivans.dcim.module.lora.domain.model.LoraIdType;
import net.vivans.dcim.module.lora.domain.model.LoraIngestErrorLog;

import java.time.Instant;

public record LoraIngestErrorLogResponse(
        Long id,
        Instant receivedAt,
        Integer deviceId,
        String deviceName,
        String externalId,
        LoraIdType idType,
        String reason,
        String rawPayload,
        boolean resolved
) {
    public static LoraIngestErrorLogResponse from(LoraIngestErrorLog log) {
        return new LoraIngestErrorLogResponse(
                log.getId(),
                log.getReceivedAt(),
                log.getDevice() == null ? null : log.getDevice().getId(),
                log.getDevice() == null ? null : log.getDevice().getName(),
                log.getExternalId(),
                log.getIdType(),
                log.getReason(),
                log.getRawPayload(),
                log.isResolved()
        );
    }
}
