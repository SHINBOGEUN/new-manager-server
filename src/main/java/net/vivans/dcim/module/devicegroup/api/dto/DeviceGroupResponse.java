package net.vivans.dcim.module.devicegroup.api.dto;

import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;

import java.util.Comparator;
import java.util.List;

public record DeviceGroupResponse(
        Integer id,
        String name,
        String description,
        boolean enabled,
        int deviceCount,
        List<DeviceGroupDeviceResponse> devices
) {
    public static DeviceGroupResponse from(DeviceGroup group) {
        List<DeviceGroupDeviceResponse> devices = group.getDevices().stream()
                .sorted(Comparator.comparing(device -> device.getId()))
                .map(DeviceGroupDeviceResponse::from)
                .toList();
        return new DeviceGroupResponse(
                group.getId(), group.getName(), group.getDescription(), group.isEnabled(), devices.size(), devices);
    }
}
