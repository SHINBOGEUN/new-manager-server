package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometricSource;

public record PageWidgetPsychrometricSourceResponse(
        Integer deviceId,
        String deviceName,
        String role,
        String pointName
) {
    public static PageWidgetPsychrometricSourceResponse from(PageWidgetPsychrometricSource source) {
        return new PageWidgetPsychrometricSourceResponse(
                source.getDevice().getId(),
                source.getDevice().getName(),
                source.getRole().name(),
                source.getPointName()
        );
    }
}
