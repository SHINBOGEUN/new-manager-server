package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.common.domain.model.CommonCode;

public record PageWidgetPageResponse(
        String code,
        String name,
        Integer sortOrder
) {
    public static PageWidgetPageResponse from(CommonCode pageCode) {
        return new PageWidgetPageResponse(
                pageCode.getCode(),
                pageCode.getName(),
                pageCode.getSortOrder()
        );
    }
}
