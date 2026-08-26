package net.vivans.dcim.module.query.api.dto;

import net.vivans.dcim.module.device.domain.model.Device;

import java.util.List;

public record LastDeviceResponse(
        Integer deviceId,
        String deviceName,
        String locationNodeCode,
        String locationNodeName,
        String deviceTypeCode,
        List<LastPointValueResponse> points
) {
    public static LastDeviceResponse from(Device device, List<LastPointValueResponse> points) {
        return new LastDeviceResponse(
                device.getId(),
                device.getName(),
                device.getLocationNode().getCode(),
                device.getLocationNode().getName(),
                device.getDeviceModel().getDeviceType().getCode(),
                points
        );
    }
}
