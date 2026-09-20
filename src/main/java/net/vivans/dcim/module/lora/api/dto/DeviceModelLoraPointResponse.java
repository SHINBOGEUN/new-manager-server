package net.vivans.dcim.module.lora.api.dto;

import net.vivans.dcim.module.lora.domain.model.DeviceModelLoraPoint;

import java.time.Instant;

public record DeviceModelLoraPointResponse(
        Integer id,
        Integer deviceModelId,
        String deviceModelName,
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
    public static DeviceModelLoraPointResponse from(DeviceModelLoraPoint point) {
        return new DeviceModelLoraPointResponse(
                point.getId(),
                point.getDeviceModel().getId(),
                point.getDeviceModel().getName(),
                point.getPayloadField(),
                point.getPointName(),
                point.getDataPointType().getId(),
                point.getDataPointType().getCode(),
                point.getUnit(),
                point.getScale(),
                point.getValueMap(),
                point.isEnabled(),
                point.getCreatedDt(),
                point.getUpdatedDt()
        );
    }
}
