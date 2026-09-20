package net.vivans.dcim.module.lora.api.dto;

import net.vivans.dcim.module.lora.domain.model.DeviceLoraPointOverride;

import java.time.Instant;

public record DeviceLoraPointOverrideResponse(
        Integer id,
        Integer deviceId,
        String deviceName,
        String payloadField,
        String pointName,
        Integer dataPointTypeId,
        String dataPointTypeCode,
        String unit,
        Double scale,
        String valueMap,
        boolean enabled,
        Instant createdDt,
        Instant updatedDt
) {
    public static DeviceLoraPointOverrideResponse from(DeviceLoraPointOverride override) {
        return new DeviceLoraPointOverrideResponse(
                override.getId(),
                override.getDevice().getId(),
                override.getDevice().getName(),
                override.getPayloadField(),
                override.getPointName(),
                override.getDataPointType().getId(),
                override.getDataPointType().getCode(),
                override.getUnit(),
                override.getScale(),
                override.getValueMap(),
                override.isEnabled(),
                override.getCreatedDt(),
                override.getUpdatedDt()
        );
    }
}
