package net.vivans.dcim.module.device.api.dto;

import java.time.Instant;
import java.util.List;

public record DevicePageMeasurementsResponse(
        String pageCode,
        String pageName,
        String viewMode,
        String layoutAssetKey,
        int lookbackHours,
        List<DeviceMeasurement> devices
) {
    public record DeviceMeasurement(
            Integer deviceId,
            String deviceName,
            Integer modelId,
            String modelName,
            String locationCode,
            String locationName,
            boolean enabled,
            List<PointMeasurement> points
    ) {
    }

    public record PointMeasurement(
            String pointName,
            String protocol,
            String unit,
            String dataPointType,
            Double value,
            Instant collectedAt
    ) {
    }
}
