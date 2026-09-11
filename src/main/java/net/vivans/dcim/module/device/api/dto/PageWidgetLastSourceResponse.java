package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;

import java.util.List;

public record PageWidgetLastSourceResponse(
        Integer deviceId,
        String deviceName,
        List<String> pointNames
) {
    public static PageWidgetLastSourceResponse from(PageWidget.LastSourceDefinition source) {
        Device device = source.device();
        return new PageWidgetLastSourceResponse(device.getId(), device.getName(), source.pointNames());
    }
}
