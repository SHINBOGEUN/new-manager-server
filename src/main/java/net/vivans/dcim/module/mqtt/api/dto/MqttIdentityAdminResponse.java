package net.vivans.dcim.module.mqtt.api.dto;

import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;

import java.time.Instant;

public record MqttIdentityAdminResponse(
        Integer id,
        Integer deviceId,
        String deviceName,
        Integer deviceModelId,
        String deviceModelName,
        MqttIdentityType idType,
        String externalId,
        String normalizedExternalId,
        boolean enabled,
        Instant createdDt,
        Instant updatedDt
) {
    public static MqttIdentityAdminResponse from(DeviceMqttIdentity endpoint) {
        return new MqttIdentityAdminResponse(
                endpoint.getId(),
                endpoint.getDevice().getId(),
                endpoint.getDevice().getName(),
                endpoint.getDevice().getDeviceModel().getId(),
                endpoint.getDevice().getDeviceModel().getName(),
                endpoint.getIdentityType(),
                endpoint.getExternalId(),
                endpoint.getNormalizedExternalId(),
                endpoint.isEnabled(),
                endpoint.getCreatedDt(),
                endpoint.getUpdatedDt()
        );
    }
}
