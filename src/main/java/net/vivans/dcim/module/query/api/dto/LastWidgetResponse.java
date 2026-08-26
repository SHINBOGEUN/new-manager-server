package net.vivans.dcim.module.query.api.dto;

import java.util.List;

public record LastWidgetResponse(
        Integer widgetId,
        String widgetName,
        String pageCode,
        List<LastDeviceResponse> devices
) {
}
