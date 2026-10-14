package net.vivans.dcim.module.mqtt.api.dto;

import java.time.Instant;
import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;
import net.vivans.dcim.module.mqtt.domain.model.MqttPointScope;

public record DeviceModelMqttPointResponse(
        Integer id, Integer deviceModelId, String deviceModelName, String payloadField, String pointName,
        Integer dataPointTypeId, String dataPointTypeCode,
        Integer categoryCodeId, String categoryCode, String categoryName, Integer unitCodeId, String unit,
        MqttPointScope componentScope, String componentType, String componentPath,
        String componentIndexField, String componentUuidField,
        Double scale, String valueMap, boolean enabled, Instant createdDt, Instant updatedDt
) {
    public static DeviceModelMqttPointResponse from(DeviceModelMqttPoint point) {
        var category = point.getCategoryCode();
        var unit = point.getUnitCode();
        return new DeviceModelMqttPointResponse(point.getId(), point.getDeviceModel().getId(),
                point.getDeviceModel().getName(), point.getPayloadField(), point.getPointName(),
                point.getDataPointType().getId(), point.getDataPointType().getCode(),
                category == null ? null : category.getId(), category == null ? null : category.getCode(),
                category == null ? null : category.getName(), unit == null ? null : unit.getId(), point.getUnit(),
                point.getComponentScope(), point.getComponentType(), point.getComponentPath(),
                point.getComponentIndexField(), point.getComponentUuidField(), point.getScale(), point.getValueMap(),
                point.isEnabled(), point.getCreatedDt(), point.getUpdatedDt());
    }
}
