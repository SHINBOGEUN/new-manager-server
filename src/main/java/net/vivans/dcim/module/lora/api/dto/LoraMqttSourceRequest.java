package net.vivans.dcim.module.lora.api.dto;

import jakarta.validation.constraints.NotBlank;
import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceType;

public record LoraMqttSourceRequest(
        @NotBlank String name,
        LoraMqttSourceType sourceType,
        @NotBlank String brokerUrl,
        @NotBlank String topic,
        String clientId,
        String credentialKey,
        Boolean enabled
) {
}
