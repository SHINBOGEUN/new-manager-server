package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.util.List;
import net.vivans.dcim.module.query.application.UnitDisplay;

public record LastWidgetResponse(
        Integer widgetId,
        String widgetName,
        String pageCode,
        /** 동일 unit 포인트들의 합 (없으면 null) */
        BigDecimal total,
        String totalUnit,
        List<LastDeviceResponse> devices,
        WidgetDataStatusResponse dataStatus,
        String displayTotal,
        String displayTotalUnit
) {
    public LastWidgetResponse(Integer widgetId, String widgetName, String pageCode, BigDecimal total,
                              String totalUnit, List<LastDeviceResponse> devices,
                              WidgetDataStatusResponse dataStatus) {
        this(widgetId, widgetName, pageCode, total, totalUnit, devices, dataStatus,
                UnitDisplay.of(total, totalUnit).displayValue(), UnitDisplay.of(total, totalUnit).displayUnit());
    }
}
