package net.vivans.dcim.module.mqtt.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;

public record MqttIdentityAdminRequest(
        @NotNull Integer deviceId,
        @NotNull MqttIdentityType idType,
        @NotBlank @jakarta.validation.constraints.Size(max = 255) String externalId,
        Boolean enabled
) {
}
