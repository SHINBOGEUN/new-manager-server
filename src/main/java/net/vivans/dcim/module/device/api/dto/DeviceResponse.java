package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.Device;

public record DeviceResponse(
        Integer id,
        Integer modelId,
        String modelName,
        String manufacturer,
        String deviceTypeCode,
        String locationNodeCode,
        String locationNodeName,
        Integer pathCodeId,
        String pathCode,
        String pathName,
        String name,
        String description,
        boolean enabled
) {

    public static DeviceResponse from(Device device) {
        Integer pathId = null;
        String pathCode = null;
        String pathName = null;
        if (device.getPathCode() != null) {
            pathId = device.getPathCode().getId();
            pathCode = device.getPathCode().getCode();
            pathName = device.getPathCode().getName();
        }
        return new DeviceResponse(
                device.getId(),
                device.getDeviceModel().getId(),
                device.getDeviceModel().getName(),
                device.getDeviceModel().getManufacturer(),
                device.getDeviceModel().getDeviceType().getCode(),
                device.getLocationNode().getCode(),
                device.getLocationNode().getName(),
                pathId,
                pathCode,
                pathName,
                device.getName(),
                device.getDescription(),
                device.isEnabled()
        );
    }
}
