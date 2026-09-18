package net.vivans.dcim.module.query.api.dto;

import java.time.Instant;
import java.util.List;

public record CollectionDeviceStatusResponse(
        Integer deviceId, String deviceName, Integer modelId, String modelName, String manufacturer,
        Integer expectedModelId, String expectedModelName,
        String locationName, String locationCode, boolean deviceEnabled,
        String protocol,
        String status, String statusMessage, String technicalDetail,
        Integer taskId, String taskName, Integer groupId, String groupName,
        String cronExpression, String collectorJobId, Long expectedIntervalSeconds,
        Instant latestCollectedAt, Long ageSeconds,
        int expectedPointCount, int availablePointCount,
        List<LatestValue> latestValues
) {
    public record LatestValue(String pointName, Double value, String unit, Instant collectedAt) {}
}
