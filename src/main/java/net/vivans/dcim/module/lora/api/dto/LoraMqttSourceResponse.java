package net.vivans.dcim.module.lora.api.dto;

import net.vivans.dcim.module.lora.domain.model.LoraMqttSource;
import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceType;

import java.time.Instant;

public record LoraMqttSourceResponse(
        Integer id,
        String name,
        LoraMqttSourceType sourceType,
        String brokerUrl,
        String topic,
        String clientId,
        String credentialKey,
        boolean enabled,
        long configVersion,
        Instant createdDt,
        Instant updatedDt,
        LoraMqttSourceStatusResponse status
) {
    public static LoraMqttSourceResponse from(LoraMqttSource source, LoraMqttSourceStatusResponse status) {
        return new LoraMqttSourceResponse(source.getId(), source.getName(), source.getSourceType(), source.getBrokerUrl(), source.getTopic(),
                source.getClientId(), source.getCredentialKey(), source.isEnabled(), source.getConfigVersion(), source.getCreatedDt(), source.getUpdatedDt(), status);
    }
}
