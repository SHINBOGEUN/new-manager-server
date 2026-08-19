package net.vivans.dcim.module.collectortask.api.dto;

import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskDevice;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record CollectionTaskGroupResponse(
        Integer id,
        String name,
        String cronExpression,
        String generatedSpec,
        String collectorJobId,
        boolean active,
        List<CollectionTaskDeviceResponse> devices,
        Instant createdDt,
        Instant updatedDt
) {

    public static CollectionTaskGroupResponse from(CollectionTaskGroup group) {
        List<CollectionTaskDeviceResponse> devices = new ArrayList<>();
        for (CollectionTaskDevice mapping : group.getDevices()) {
            devices.add(CollectionTaskDeviceResponse.from(mapping));
        }
        return new CollectionTaskGroupResponse(
                group.getId(),
                group.getName(),
                group.getCronExpression(),
                group.getGeneratedSpec(),
                group.getCollectorJobId(),
                group.isActive(),
                devices,
                group.getCreatedDt(),
                group.getUpdatedDt()
        );
    }
}
