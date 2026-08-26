package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.PageWidgetLayout;

public record PageWidgetLayoutResponse(
        int gridX,
        int gridY,
        int w,
        int h
) {
    public static PageWidgetLayoutResponse from(PageWidgetLayout layout) {
        if (layout == null) {
            return null;
        }
        return new PageWidgetLayoutResponse(
                layout.getGridX(),
                layout.getGridY(),
                layout.getW(),
                layout.getH()
        );
    }
}
