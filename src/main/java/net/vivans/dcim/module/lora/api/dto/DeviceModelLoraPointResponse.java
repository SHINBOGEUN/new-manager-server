package net.vivans.dcim.module.lora.api.dto;

import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;

import java.time.Instant;

public record DeviceModelLoraPointResponse(
        Integer id,
        Integer deviceModelId,
        String deviceModelName,
        String payloadField,
        String pointName,
        Integer dataPointTypeId,
        String dataPointTypeCode,
        Integer categoryCodeId,
        String categoryCode,
        String categoryName,
        Integer unitCodeId,
        String unit,
        Double scale,
        String valueMap,
        boolean enabled,
        Instant createdDt,
        Instant updatedDt
) {
    public static DeviceModelLoraPointResponse from(DeviceModelMqttPoint point) {
        return new DeviceModelLoraPointResponse(
                point.getId(),
                point.getDeviceModel().getId(),
                point.getDeviceModel().getName(),
                point.getPayloadField(),
                point.getPointName(),
                point.getDataPointType().getId(),
                point.getDataPointType().getCode(),
                point.getCategoryCode() == null ? null : point.getCategoryCode().getId(),
                point.getCategoryCode() == null ? null : point.getCategoryCode().getCode(),
                point.getCategoryCode() == null ? null : point.getCategoryCode().getName(),
                point.getUnitCode() == null ? null : point.getUnitCode().getId(),
                point.getUnit(),
                point.getScale(),
                point.getValueMap(),
                point.isEnabled(),
                point.getCreatedDt(),
                point.getUpdatedDt()
        );
    }
}
