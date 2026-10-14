package net.vivans.dcim.module.mqtt.api.dto;

import jakarta.validation.constraints.NotBlank;
import net.vivans.dcim.module.mqtt.domain.model.MqttSourceType;

public record MqttSourceRequest(
        @NotBlank String name,
        MqttSourceType sourceType,
        @NotBlank String brokerUrl,
        @NotBlank String topic,
        String clientId,
        String credentialKey,
        Boolean enabled
) {
}
