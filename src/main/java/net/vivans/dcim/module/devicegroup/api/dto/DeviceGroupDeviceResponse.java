package net.vivans.dcim.module.devicegroup.api.dto;

import net.vivans.dcim.module.device.domain.model.Device;

public record DeviceGroupDeviceResponse(
        Integer id,
        String name,
        Integer modelId,
        String modelName,
        String locationNodeName,
        boolean enabled
) {
    public static DeviceGroupDeviceResponse from(Device device) {
        return new DeviceGroupDeviceResponse(
                device.getId(),
                device.getName(),
                device.getDeviceModel().getId(),
                device.getDeviceModel().getName(),
                device.getLocationNode().getName(),
                device.isEnabled()
        );
    }
}
