package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.PageWidgetPueSource;

public record PageWidgetPueSourceResponse(Integer deviceId, String deviceName, String role, String pointName) {
    public static PageWidgetPueSourceResponse from(PageWidgetPueSource source) {
        return new PageWidgetPueSourceResponse(source.getDevice().getId(), source.getDevice().getName(),
                source.getRole().name(), source.getPointName());
    }
}
