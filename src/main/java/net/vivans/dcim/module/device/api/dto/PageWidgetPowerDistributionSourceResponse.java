package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.PageWidgetPowerDistributionSource;

public record PageWidgetPowerDistributionSourceResponse(
        Integer deviceId,
        String deviceName,
        String pointName
) {
    public static PageWidgetPowerDistributionSourceResponse from(PageWidgetPowerDistributionSource source) {
        return new PageWidgetPowerDistributionSourceResponse(
                source.getDevice().getId(), source.getDevice().getName(), source.getPointName());
    }
}
