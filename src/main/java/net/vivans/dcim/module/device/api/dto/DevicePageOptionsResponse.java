package net.vivans.dcim.module.device.api.dto;

import java.util.List;

public record DevicePageOptionsResponse(
        String pageCode,
        String pageName,
        List<DeviceOption> devices
) {
    public record DeviceOption(
            Integer deviceId,
            String deviceName,
            String type,
            Integer modelId,
            String modelName,
            String locationCode,
            String locationName,
            boolean enabled,
            List<PointOption> points
    ) {
    }

    public record PointOption(
            Integer protocolTypeId,
            String protocol,
            Integer pointId,
            Integer sourceModelId,
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
            String origin,
            int sortOrder,
            String componentScope,
            String componentType
    ) {
        public PointOption(Integer protocolTypeId, String protocol, Integer pointId, Integer sourceModelId, String pointName, String unit, String valueType, Integer dataPointTypeId, String dataPointType, Integer categoryCodeId, String categoryCode, String categoryName, Boolean requiresInstance, boolean collectionEnabled, String origin, int sortOrder) {
            this(protocolTypeId, protocol, pointId, sourceModelId, pointName, unit, valueType, dataPointTypeId, dataPointType, categoryCodeId, categoryCode, categoryName, requiresInstance, collectionEnabled, origin, sortOrder, "DEVICE", null);
        }
    }
}
