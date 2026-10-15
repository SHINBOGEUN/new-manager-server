package net.vivans.dcim.module.mqtt.api.dto;

import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;

public record DeviceMqttIdentityResponse(
        Integer id, MqttIdentityType identityType, String externalId,
        String normalizedExternalId, boolean enabled
) {
    public static DeviceMqttIdentityResponse from(DeviceMqttIdentity identity) {
        return new DeviceMqttIdentityResponse(identity.getId(), identity.getIdentityType(),
                identity.getExternalId(), identity.getNormalizedExternalId(), identity.isEnabled());
    }
}
