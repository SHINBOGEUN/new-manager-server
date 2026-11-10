package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import net.vivans.dcim.module.query.application.UnitDisplay;

public record AggregateWidgetResponse(
        Integer widgetId,
        String widgetName,
        String pageCode,
        String aggregatePreset,
        String rangePreset,
        Instant start,
        Instant end,
        /** 전체 합 */
        BigDecimal value,
        String unit,
        Integer contributingDevices,
        /** 장비별 기여 값 */
        List<AggregateDeviceValueResponse> devices,
        WidgetDataStatusResponse dataStatus,
        Instant peakTime,
        String displayValue,
        String displayUnit
) {
    public AggregateWidgetResponse(Integer widgetId, String widgetName, String pageCode, String aggregatePreset,
            String rangePreset, Instant start, Instant end, BigDecimal value, String unit, Integer contributingDevices,
            List<AggregateDeviceValueResponse> devices, WidgetDataStatusResponse dataStatus, Instant peakTime) {
        this(widgetId, widgetName, pageCode, aggregatePreset, rangePreset, start, end, value, unit,
                contributingDevices, devices, dataStatus, peakTime,
                UnitDisplay.of(value, unit).displayValue(), UnitDisplay.of(value, unit).displayUnit());
    }
    public AggregateWidgetResponse(Integer widgetId, String widgetName, String pageCode, String aggregatePreset,
            String rangePreset, Instant start, Instant end, BigDecimal value, String unit, Integer contributingDevices,
            List<AggregateDeviceValueResponse> devices, WidgetDataStatusResponse dataStatus) {
        this(widgetId, widgetName, pageCode, aggregatePreset, rangePreset, start, end, value, unit,
                contributingDevices, devices, dataStatus, null);
    }
}
