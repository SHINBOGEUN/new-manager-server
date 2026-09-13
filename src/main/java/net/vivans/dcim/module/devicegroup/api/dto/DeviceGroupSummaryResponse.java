package net.vivans.dcim.module.devicegroup.api.dto;

import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;

public record DeviceGroupSummaryResponse(Integer id, String name, boolean enabled) {

    public static DeviceGroupSummaryResponse from(DeviceGroup group) {
        return new DeviceGroupSummaryResponse(group.getId(), group.getName(), group.isEnabled());
    }
}
