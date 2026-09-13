package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.PageWidgetPueSource;
import net.vivans.dcim.module.pue.domain.model.PueDefinitionSource;
import net.vivans.dcim.module.pue.domain.model.PueDefinition;

public record PageWidgetPueSourceResponse(Integer deviceId, String deviceName, String role, String pointName) {
    public static PageWidgetPueSourceResponse from(PageWidgetPueSource source) {
        return new PageWidgetPueSourceResponse(source.getDevice().getId(), source.getDevice().getName(),
                source.getRole().name(), source.getPointName());
    }

    public static PageWidgetPueSourceResponse from(PueDefinitionSource source) {
        return new PageWidgetPueSourceResponse(source.getDevice().getId(), source.getDevice().getName(),
                source.getRole().name(), source.getPointName());
    }

    public static PageWidgetPueSourceResponse from(PueDefinition.SourceDefinition source) {
        return new PageWidgetPueSourceResponse(source.device().getId(), source.device().getName(),
                source.role().name(), source.pointName());
    }
}
