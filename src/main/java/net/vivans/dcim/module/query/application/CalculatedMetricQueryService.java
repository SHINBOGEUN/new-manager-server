package net.vivans.dcim.module.query.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.calculated.domain.model.CalculatedMetric;
import net.vivans.dcim.module.query.api.dto.CalculatedMetricQueryResponse;
import net.vivans.dcim.module.query.api.dto.CalculatedMetricTrendPointResponse;
import net.vivans.dcim.module.query.api.dto.WidgetDataStatusResponse;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.CalculatedMetricLastPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalculatedMetricQueryService {
    private final PointQuery pointQuery;
    private final PageWidgetRepository pageWidgetRepository;
    private final WidgetDataStatusResolver widgetDataStatusResolver;

    public CalculatedMetricQueryResponse getCalculated(Integer widgetId, String rangePresetOverride, String windowOverride) {
        return getCalculated(widgetId, rangePresetOverride, windowOverride, null);
    }

    public CalculatedMetricQueryResponse getCalculated(Integer widgetId, String rangePresetOverride, String windowOverride, String compareMode) {
        PageWidget widget = PageWidgetFinder.findRequired(pageWidgetRepository, widgetId);
        if (widget.getQueryKind() != PageWidgetQueryKind.calculated) {
            throw new IllegalArgumentException("queryKind must be calculated");
        }
        if (!widget.isEnabled()) throw new IllegalArgumentException("widget is disabled");
        if (widget.getCalculatedMetricId() == null) throw new IllegalArgumentException("calculated widget has no metric");

        CalculatedMetric definition = widget.getCalculated().getCalculatedMetric();
        PageWidgetChartRangePreset preset = rangePresetOverride == null || rangePresetOverride.isBlank()
                ? widget.getCalculatedRangePreset()
                : PageWidgetChartRangePreset.from(rangePresetOverride);
        if (preset == null) preset = PageWidgetChartRangePreset.last_24h;
        QueryRanges.Range range = QueryRanges.resolve(preset);
        Duration lookback = Duration.between(range.start(), range.end());
        CalculatedMetricLastPoint currentPoint = pointQuery.findLastCalculated(
                definition.getId(), definition.getConfigVersion(), lookback).orElse(null);
        Duration previousLookback = lookback.compareTo(Duration.ofDays(3)) < 0 ? Duration.ofDays(3) : lookback;
        CalculatedMetricLastPoint previousPoint = currentPoint == null
                ? pointQuery.findLastCalculatedPreviousVersion(definition.getId(),
                    definition.getConfigVersion(), previousLookback).orElse(null) : null;
        boolean previousVersion = currentPoint == null && previousPoint != null;
        boolean stale = currentPoint != null && widget.getCalculatedFreshnessMinutes() != null
                && currentPoint.time().isBefore(Instant.now().minusSeconds(widget.getCalculatedFreshnessMinutes().longValue() * 60));
        boolean complete = currentPoint != null && !stale;
        CalculatedMetricLastPoint displayedPoint = previousVersion ? previousPoint : currentPoint;
        WidgetDataStatusResponse resolvedStatus = widgetDataStatusResolver.resolve(
                Collections.singletonList(displayedPoint == null ? null : displayedPoint.time()),
                widget.getCalculatedFreshnessMinutes());
        WidgetDataStatusResponse status = previousVersion
                ? new WidgetDataStatusResponse("PREVIOUS", resolvedStatus.latestCollectedAt(),
                    resolvedStatus.freshnessMinutes(), resolvedStatus.expectedSourceCount(),
                    resolvedStatus.availableSourceCount(), resolvedStatus.missingSourceCount(),
                    resolvedStatus.staleSourceCount())
                : resolvedStatus;
        List<CalculatedMetricTrendPointResponse> trend = List.of();
        if ((rangePresetOverride != null && !rangePresetOverride.isBlank())
                || (windowOverride != null && !windowOverride.isBlank())) {
            String window = windowOverride == null || windowOverride.isBlank() ? "15m" : windowOverride.trim();
            if (!java.util.Set.of("1m", "5m", "15m", "1h", "1d").contains(window)) {
                throw new IllegalArgumentException("window must be 1m, 5m, 15m, 1h, or 1d");
            }
            trend = pointQuery.findCalculatedSeries(definition.getId(), range.start(), range.end(), window)
                    .stream().map(item -> new CalculatedMetricTrendPointResponse(item.time(), QueryValues.round4(item.value())))
                    .toList();
            ChartComparisonSupport.Comparison comparison = ChartComparisonSupport.resolve(compareMode, preset, range);
            if (comparison != null) {
                List<CalculatedMetricTrendPointResponse> previous = pointQuery.findCalculatedSeries(
                                definition.getId(), comparison.range().start(), comparison.range().end(), window)
                        .stream().map(item -> new CalculatedMetricTrendPointResponse(
                                item.time().plusSeconds(comparison.offsetSeconds()), QueryValues.round4(item.value()),
                                true, comparison.offsetSeconds(), item.time(), comparison.label()))
                        .toList();
                trend = java.util.stream.Stream.concat(trend.stream(), previous.stream()).toList();
            }
        }
        return new CalculatedMetricQueryResponse(previousVersion ? QueryValues.round4(previousPoint.value())
                    : complete ? QueryValues.round4(currentPoint.value()) : null,
                previousVersion ? null : definition.getResultUnit(), preset.name(), range.start(), range.end(), complete,
                previousVersion ? "PREVIOUS_VERSION" : currentPoint == null ? "MISSING_DATA" : stale ? "STALE_DATA" : "OK",
                trend, status, definition.getFormula(),
                complete ? currentPoint.inputs() : Map.of());
    }
}
