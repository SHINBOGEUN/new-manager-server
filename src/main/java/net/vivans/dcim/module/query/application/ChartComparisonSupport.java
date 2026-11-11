package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.query.api.dto.ChartSeriesResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

final class ChartComparisonSupport {
    private ChartComparisonSupport() {}

    static Comparison resolve(String rawMode, PageWidgetChartRangePreset preset, QueryRanges.Range current) {
        String mode = rawMode == null || rawMode.isBlank() ? "none" : rawMode.trim();
        if ("none".equals(mode)) return null;
        Instant start;
        Instant end;
        String label;
        switch (mode) {
            case "previous_day" -> {
                start = current.start().minus(Duration.ofDays(1));
                end = current.end().minus(Duration.ofDays(1));
                label = "전날";
            }
            case "previous_week" -> {
                start = current.start().minus(Duration.ofDays(7));
                end = current.end().minus(Duration.ofDays(7));
                label = "지난주";
            }
            case "previous_period" -> {
                label = switch (preset) {
                    case today -> "어제";
                    case yesterday -> "그저께";
                    case this_month -> "지난달";
                    case last_month -> "전전달";
                    default -> "직전 기간";
                };
                if (preset == PageWidgetChartRangePreset.today || preset == PageWidgetChartRangePreset.yesterday) {
                    start = current.start().minus(Duration.ofDays(1));
                    end = current.end().minus(Duration.ofDays(1));
                } else if (preset == PageWidgetChartRangePreset.this_month) {
                    LocalDate currentMonthStart = current.start().atZone(ZoneOffset.UTC).toLocalDate();
                    LocalDate previousMonthStart = currentMonthStart.minusMonths(1).withDayOfMonth(1);
                    start = previousMonthStart.atStartOfDay(ZoneOffset.UTC).toInstant();
                    end = current.end().minus(Duration.between(
                            currentMonthStart.atStartOfDay(ZoneOffset.UTC).toInstant(),
                            previousMonthStart.atStartOfDay(ZoneOffset.UTC).toInstant()));
                } else if (preset == PageWidgetChartRangePreset.last_month) {
                    LocalDate lastMonthStart = current.start().atZone(ZoneOffset.UTC).toLocalDate();
                    start = lastMonthStart.minusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
                    end = current.start();
                } else {
                    Duration span = Duration.between(current.start(), current.end());
                    start = current.start().minus(span);
                    end = current.start();
                }
            }
            default -> throw new IllegalArgumentException("compare must be none, previous_period, previous_day, or previous_week");
        }
        long offset = Duration.between(start, current.start()).getSeconds();
        return new Comparison(mode, label, offset, new QueryRanges.Range(start, end));
    }

    static List<ChartSeriesResponse> appendComparison(List<ChartSeriesResponse> current,
                                                       List<ChartSeriesResponse> previous,
                                                       Comparison comparison) {
        if (comparison == null) return current;
        List<ChartSeriesResponse> result = new java.util.ArrayList<>(current);
        for (ChartSeriesResponse series : previous) {
            List<Instant> original = series.times();
            List<Instant> shifted = original.stream().map(time -> time.plusSeconds(comparison.offsetSeconds())).toList();
            result.add(new ChartSeriesResponse(series.key() + ":compare", series.label() + " (" + comparison.label() + ")",
                    series.deviceId(), series.pointName(), series.locationNodeCode(), series.unit(), series.axis(),
                    shifted, series.values(), true, comparison.offsetSeconds(), original));
        }
        return List.copyOf(result);
    }

    record Comparison(String mode, String label, long offsetSeconds, QueryRanges.Range range) {}
}
