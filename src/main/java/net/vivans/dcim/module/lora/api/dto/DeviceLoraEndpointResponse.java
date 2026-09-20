package net.vivans.dcim.module.lora.api.dto;

import net.vivans.dcim.module.lora.domain.model.DeviceLoraEndpoint;
import net.vivans.dcim.module.lora.domain.model.LoraIdType;

import java.time.Instant;

public record DeviceLoraEndpointResponse(
        Integer id,
        Integer deviceId,
        String deviceName,
        Integer deviceModelId,
        String deviceModelName,
        LoraIdType idType,
        String externalId,
        String normalizedExternalId,
        boolean enabled,
        Instant createdDt,
        Instant updatedDt
) {
    public static DeviceLoraEndpointResponse from(DeviceLoraEndpoint endpoint) {
        return new DeviceLoraEndpointResponse(
                endpoint.getId(),
                endpoint.getDevice().getId(),
                endpoint.getDevice().getName(),
                endpoint.getDevice().getDeviceModel().getId(),
                endpoint.getDevice().getDeviceModel().getName(),
                endpoint.getIdType(),
                endpoint.getExternalId(),
                endpoint.getNormalizedExternalId(),
                endpoint.isEnabled(),
                endpoint.getCreatedDt(),
                endpoint.getUpdatedDt()
        );
    }
}
