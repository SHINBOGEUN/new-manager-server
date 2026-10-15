package net.vivans.dcim.module.devicemodel.api.dto;

import java.util.List;

public record DeviceModelMeasurementPointOptionsResponse(
        Integer modelId,
        String modelName,
        List<PointOption> points
) {
    public record PointOption(
            Integer pointId,
            Integer protocolTypeId,
            String protocol,
            String pointName,
            String unit,
            String valueType,
            Integer dataPointTypeId,
            String dataPointType,
            Integer categoryCodeId,
            String categoryCode,
            String categoryName,
            Boolean requiresInstance,
            boolean collectionEnabled,
            String componentScope,
            String componentType
    ) {
        public PointOption(Integer pointId, Integer protocolTypeId, String protocol, String pointName, String unit, String valueType, Integer dataPointTypeId, String dataPointType, Integer categoryCodeId, String categoryCode, String categoryName, Boolean requiresInstance, boolean collectionEnabled) {
            this(pointId, protocolTypeId, protocol, pointName, unit, valueType, dataPointTypeId, dataPointType, categoryCodeId, categoryCode, categoryName, requiresInstance, collectionEnabled, "DEVICE", null);
        }
    }
}
