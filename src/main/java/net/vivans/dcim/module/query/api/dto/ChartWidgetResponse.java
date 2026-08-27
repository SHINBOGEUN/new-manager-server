package net.vivans.dcim.module.query.api.dto;

import java.time.Instant;
import java.util.List;

public record ChartWidgetResponse(
        Integer widgetId,
        String widgetName,
        String pageCode,
        String chartScope,
        String seriesMode,
        String rangePreset,
        String window,
        Instant start,
        Instant end,
        String unit,
        List<ChartSeriesResponse> series
) {
}
