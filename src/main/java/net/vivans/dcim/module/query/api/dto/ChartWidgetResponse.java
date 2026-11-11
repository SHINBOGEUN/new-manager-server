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
        List<String> units,
        List<ChartSeriesResponse> series,
        WidgetDataStatusResponse dataStatus,
        List<String> availableSeriesModes,
        List<UnitGroup> unitGroups
) {
    public record UnitGroup(String unit, List<ChartSeriesResponse> series) {}

    public ChartWidgetResponse(Integer widgetId, String widgetName, String pageCode, String chartScope,
                               String seriesMode, String rangePreset, String window, Instant start, Instant end,
                               String unit, List<String> units, List<ChartSeriesResponse> series,
                               WidgetDataStatusResponse dataStatus, List<String> availableSeriesModes) {
        this(widgetId, widgetName, pageCode, chartScope, seriesMode, rangePreset, window, start, end,
                unit, units, series, dataStatus, availableSeriesModes, groupByUnit(units, series));
    }

    public ChartWidgetResponse(Integer widgetId, String widgetName, String pageCode, String chartScope,
                               String seriesMode, String rangePreset, String window, Instant start, Instant end,
                               String unit, List<String> units, List<ChartSeriesResponse> series,
                               WidgetDataStatusResponse dataStatus) {
        this(widgetId, widgetName, pageCode, chartScope, seriesMode, rangePreset, window, start, end,
                unit, units, series, dataStatus, List.of("sum", "per_device"), groupByUnit(units, series));
    }

    private static List<UnitGroup> groupByUnit(List<String> units, List<ChartSeriesResponse> series) {
        if (series == null || series.isEmpty()) return List.of();
        java.util.Map<String, List<ChartSeriesResponse>> grouped = new java.util.LinkedHashMap<>();
        for (ChartSeriesResponse item : series) {
            String unit = item.unit() == null || item.unit().isBlank() ? "(단위 없음)" : item.unit();
            grouped.computeIfAbsent(unit, ignored -> new java.util.ArrayList<>()).add(item);
        }
        return grouped.entrySet().stream().map(entry -> new UnitGroup(entry.getKey(), List.copyOf(entry.getValue()))).toList();
    }
}
