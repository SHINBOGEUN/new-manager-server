package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record LastWidgetResponse(
        Integer widgetId,
        String widgetName,
        String pageCode,
        /** 동일 unit 포인트들의 합 (없으면 null) */
        BigDecimal total,
        String totalUnit,
        List<LastDeviceResponse> devices
) {
}
