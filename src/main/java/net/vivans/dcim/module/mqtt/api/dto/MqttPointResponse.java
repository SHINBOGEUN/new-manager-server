package net.vivans.dcim.module.mqtt.api.dto;

import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;
import net.vivans.dcim.module.mqtt.domain.model.MqttPointScope;

public record MqttPointResponse(
        Integer id, Integer deviceModelId, String payloadField, String pointName,
        Integer dataPointTypeId, String dataPointTypeCode,
        Integer categoryCodeId, String categoryName,
        Integer unitCodeId, String unit, Double scale, String valueMap, boolean enabled,
        MqttPointScope componentScope, String componentType, String componentPath,
        String componentIndexField, String componentUuidField
) {
    public static MqttPointResponse from(DeviceModelMqttPoint p) {
        return new MqttPointResponse(p.getId(), p.getDeviceModel().getId(), p.getPayloadField(),
                p.getPointName(), p.getDataPointType().getId(), p.getDataPointType().getCode(),
                p.getCategoryCode() == null ? null : p.getCategoryCode().getId(), p.getCategory(),
                p.getUnitCode() == null ? null : p.getUnitCode().getId(), p.getUnit(), p.getScale(),
                p.getValueMap(), p.isEnabled(), p.getComponentScope(), p.getComponentType(),
                p.getComponentPath(), p.getComponentIndexField(), p.getComponentUuidField());
    }
}
