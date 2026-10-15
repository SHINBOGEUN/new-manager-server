package net.vivans.dcim.module.mqtt.api.dto;

import net.vivans.dcim.module.mqtt.domain.model.MqttSource;
import net.vivans.dcim.module.mqtt.domain.model.MqttSourceType;

import java.time.Instant;

public record MqttSourceResponse(
        Integer id,
        String name,
        MqttSourceType sourceType,
        String brokerUrl,
        String topic,
        String clientId,
        String credentialKey,
        boolean enabled,
        long configVersion,
        Instant createdDt,
        Instant updatedDt,
        MqttSourceStatusResponse status
) {
    public static MqttSourceResponse from(MqttSource source, MqttSourceStatusResponse status) {
        return new MqttSourceResponse(source.getId(), source.getName(), source.getSourceType(), source.getBrokerUrl(), source.getTopic(),
                source.getClientId(), source.getCredentialKey(), source.isEnabled(), source.getConfigVersion(), source.getCreatedDt(), source.getUpdatedDt(), status);
    }
}
