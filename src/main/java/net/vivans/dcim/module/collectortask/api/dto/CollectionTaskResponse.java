package net.vivans.dcim.module.collectortask.api.dto;

import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record CollectionTaskResponse(
        Integer id,
        String name,
        Integer modelId,
        String modelName,
        Integer scriptTypeId,
        String scriptTypeCode,
        String scriptTypeName,
        boolean active,
        List<CollectionTaskGroupResponse> groups,
        Instant createdDt,
        Instant updatedDt
) {

    public static CollectionTaskResponse from(CollectionTask task) {
        List<CollectionTaskGroupResponse> groups = new ArrayList<>();
        for (CollectionTaskGroup group : task.getGroups()) {
            groups.add(CollectionTaskGroupResponse.from(group));
        }
        return new CollectionTaskResponse(
                task.getId(),
                task.getName(),
                task.getDeviceModel().getId(),
                task.getDeviceModel().getName(),
                task.getScriptType().getId(),
                task.getScriptType().getCode(),
                task.getScriptType().getName(),
                task.isActive(),
                groups,
                task.getCreatedDt(),
                task.getUpdatedDt()
        );
    }
}
