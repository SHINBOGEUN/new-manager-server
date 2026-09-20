package net.vivans.dcim.module.lora.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.vivans.dcim.module.lora.domain.model.LoraIdType;

public record DeviceLoraEndpointRequest(
        @NotNull Integer deviceId,
        @NotNull LoraIdType idType,
        @NotBlank String externalId,
        Boolean enabled
) {
}
