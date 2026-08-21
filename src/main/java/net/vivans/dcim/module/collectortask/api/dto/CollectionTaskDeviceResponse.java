package net.vivans.dcim.module.collectortask.api.dto;

import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskDevice;

public record CollectionTaskDeviceResponse(
        Integer deviceId,
        String deviceName
) {

    public static CollectionTaskDeviceResponse from(CollectionTaskDevice mapping) {
        return new CollectionTaskDeviceResponse(
                mapping.getDevice().getId(),
                mapping.getDevice().getName()
        );
    }
}
