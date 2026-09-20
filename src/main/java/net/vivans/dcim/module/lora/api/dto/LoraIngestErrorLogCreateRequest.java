package net.vivans.dcim.module.lora.api.dto;

import jakarta.validation.constraints.NotBlank;
import net.vivans.dcim.module.lora.domain.model.LoraIdType;

import java.time.Instant;

public record LoraIngestErrorLogCreateRequest(
        Instant receivedAt,
        Integer deviceId,
        String externalId,
        LoraIdType idType,
        @NotBlank String reason,
        String rawPayload
) {
}
