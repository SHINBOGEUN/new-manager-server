package net.vivans.dcim.module.mqtt.api.dto;

import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;

public record MqttIdentityLookupResponse(
        Integer identityId, MqttIdentityType identityType, String externalId, String normalizedExternalId,
        Integer deviceId, String deviceName, Integer deviceModelId, String deviceModelName, boolean enabled
) {
    public static MqttIdentityLookupResponse from(DeviceMqttIdentity identity) {
        var device = identity.getDevice();
        var model = device.getDeviceModel();
        return new MqttIdentityLookupResponse(identity.getId(), identity.getIdentityType(),
                identity.getExternalId(), identity.getNormalizedExternalId(), device.getId(), device.getName(),
                model.getId(), model.getName(), identity.isEnabled() && device.isEnabled());
    }
}
