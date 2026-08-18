package net.vivans.dcim.module.collectortask.api.dto;

import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;

import java.time.Instant;

public record CollectionTaskResponse(
        String id,
        String name,
        String cronExpression,
        Integer scriptTypeId,
        String scriptTypeCode,
        String scriptTypeName,
        String generatedScript,
        String collectorTaskId,
        boolean active,
        Instant createdDt,
        Instant updatedDt
) {

    public static CollectionTaskResponse from(CollectionTask collectionTask) {
        return new CollectionTaskResponse(
                collectionTask.getId(),
                collectionTask.getName(),
                collectionTask.getCronExpression(),
                collectionTask.getScriptType().getId(),
                collectionTask.getScriptType().getCode(),
                collectionTask.getScriptType().getName(),
                collectionTask.getGeneratedScript(),
                collectionTask.getCollectorTaskId(),
                collectionTask.isActive(),
                collectionTask.getCreatedDt(),
                collectionTask.getUpdatedDt()
        );
    }
}
