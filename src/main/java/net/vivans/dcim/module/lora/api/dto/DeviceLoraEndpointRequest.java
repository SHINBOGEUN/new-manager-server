package net.vivans.dcim.module.lora.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;

public record DeviceLoraEndpointRequest(
        @NotNull Integer deviceId,
        @NotNull MqttIdentityType idType,
        @NotBlank String externalId,
        Boolean enabled
) {
}
