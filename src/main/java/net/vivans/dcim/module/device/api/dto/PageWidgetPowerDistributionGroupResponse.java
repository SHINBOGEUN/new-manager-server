package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.PageWidgetPowerDistributionGroup;

import java.util.List;

public record PageWidgetPowerDistributionGroupResponse(
        Integer id,
        String name,
        String color,
        List<PageWidgetPowerDistributionSourceResponse> sources
) {
    public static PageWidgetPowerDistributionGroupResponse from(PageWidgetPowerDistributionGroup group) {
        return new PageWidgetPowerDistributionGroupResponse(
                group.getId(), group.getName(), group.getColor(),
                group.getSources().stream().map(PageWidgetPowerDistributionSourceResponse::from).toList());
    }
}
