package net.vivans.dcim.module.query.api.dto;

import java.time.Instant;
import java.util.List;
import net.vivans.dcim.module.collectortask.application.CollectionReadiness;

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
        List<LatestValue> latestValues,
        List<CollectionReadiness> readiness,
        net.vivans.dcim.module.collectortask.application.CollectionRuntimeState collectionState
) {
    public CollectionDeviceStatusResponse(Integer deviceId, String deviceName, Integer modelId, String modelName, String manufacturer,
        Integer expectedModelId, String expectedModelName,
        String locationName, String locationCode, boolean deviceEnabled,
        String protocol,
        String status, String statusMessage, String technicalDetail,
        Integer taskId, String taskName, Integer groupId, String groupName,
        String cronExpression, String collectorJobId, Long expectedIntervalSeconds,
        Instant latestCollectedAt, Long ageSeconds,
        int expectedPointCount, int availablePointCount,
        List<LatestValue> latestValues,
        List<CollectionReadiness> readiness){this(deviceId,deviceName,modelId,modelName,manufacturer,expectedModelId,expectedModelName,locationName,locationCode,deviceEnabled,protocol,status,statusMessage,technicalDetail,taskId,taskName,groupId,groupName,cronExpression,collectorJobId,expectedIntervalSeconds,latestCollectedAt,ageSeconds,expectedPointCount,availablePointCount,latestValues,readiness,null);}

    public record LatestValue(Integer targetDeviceId, String targetDeviceName,
                              String pointName, Double value, String unit, Instant collectedAt) {}
}
