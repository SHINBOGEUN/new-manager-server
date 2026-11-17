package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.query.api.dto.ChartSeriesResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ChartComparisonSupport {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private ChartComparisonSupport() {}

    static Comparison resolve(String rawMode, PageWidgetChartRangePreset preset, QueryRanges.Range ignored) {
        String mode = canonicalMode(rawMode, preset);
        if ("none".equals(mode)) return null;

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate currentDate;
        LocalDate previousDate;
        String currentLabel;
        String previousLabel;
        switch (mode) {
            case "today_yesterday" -> {
                currentDate = today;
                previousDate = today.minusDays(1);
                currentLabel = "오늘";
                previousLabel = "어제";
            }
            case "today_previous_month" -> {
                currentDate = today;
                previousDate = today.minusMonths(1);
                currentLabel = "오늘";
                previousLabel = "전월 동일일";
            }
            case "week_lastweek" -> {
                currentDate = today.with(java.time.DayOfWeek.MONDAY);
                previousDate = currentDate.minusWeeks(1);
                currentLabel = "이번 주";
                previousLabel = "지난주";
            }
            case "month_lastmonth" -> {
                currentDate = today.withDayOfMonth(1);
                previousDate = currentDate.minusMonths(1);
                currentLabel = "이번 달";
                previousLabel = "지난달";
            }
            default -> throw new IllegalArgumentException("compare must be none, today_yesterday, week_lastweek, or month_lastmonth");
        }
        Instant now = Instant.now();
        Instant currentStart = currentDate.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant previousStart = previousDate.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant currentEnd = now;
        Instant previousEnd;
        if ("month_lastmonth".equals(mode)) {
            LocalDateTime currentLocal = now.atZone(BUSINESS_ZONE).toLocalDateTime();
            YearMonth previousMonth = YearMonth.from(previousDate);
            LocalDateTime previousLocalEnd = previousMonth.atDay(Math.min(currentLocal.getDayOfMonth(), previousMonth.lengthOfMonth()))
                    .atTime(currentLocal.toLocalTime());
            previousEnd = previousLocalEnd.atZone(BUSINESS_ZONE).toInstant();
        } else {
            previousEnd = previousStart.plus(Duration.between(currentStart, currentEnd));
        }
        long offset = Duration.between(previousStart, currentStart).getSeconds();
        return new Comparison(mode, currentLabel, previousLabel, offset,
                new QueryRanges.Range(previousStart, previousEnd),
                new QueryRanges.Range(currentStart, currentEnd));
    }

    static String canonicalMode(String rawMode, PageWidgetChartRangePreset preset) {
        String mode = rawMode == null || rawMode.isBlank() ? "none" : rawMode.trim();
        return switch (mode) {
            case "none" -> "none";
            case "today_yesterday", "previous_day" -> "today_yesterday";
            case "week_lastweek", "previous_week" -> "week_lastweek";
            case "month_lastmonth" -> "month_lastmonth";
            case "today_previous_month" -> "today_previous_month";
            case "previous_period" -> preset == PageWidgetChartRangePreset.this_month
                    || preset == PageWidgetChartRangePreset.last_month ? "month_lastmonth"
                    : preset == PageWidgetChartRangePreset.last_7d ? "week_lastweek" : "today_yesterday";
            default -> throw new IllegalArgumentException("compare must be none, today_yesterday, week_lastweek, or month_lastmonth");
        };
    }

    static String currentLabel(Comparison comparison) {
        return comparison == null ? "현재" : comparison.currentLabel();
    }

    static String windowFor(PageWidgetChartRangePreset preset) {
        return switch (preset) {
            case last_1h -> "1m";
            case last_6h, last_24h, today, yesterday -> "5m";
            case last_3d -> "15m";
            case last_7d -> "1h";
            case this_month, last_month -> "1d";
        };
    }

    static String windowForComparison(Comparison comparison, PageWidgetChartRangePreset preset) {
        if (comparison == null) return windowFor(preset);
        return switch (comparison.mode()) {
            case "week_lastweek" -> "1h";
            case "month_lastmonth" -> "1d";
            default -> "5m";
        };
    }

    static String requestedWindow(String requested, Comparison comparison, PageWidgetChartRangePreset preset) {
        if (requested == null || requested.isBlank()) return windowForComparison(comparison, preset);
        String value = requested.trim();
        if (!java.util.Set.of("1m", "5m", "15m", "1h", "1d").contains(value)) {
            throw new IllegalArgumentException("window must be 1m, 5m, 15m, 1h, or 1d");
        }
        return value;
    }

    static List<ChartSeriesResponse> sum(List<ChartSeriesResponse> sources, String unit, String label) {
        Map<Instant, Double> values = new LinkedHashMap<>();
        for (ChartSeriesResponse source : sources) {
            for (int index = 0; index < source.times().size(); index++) {
                Double value = source.values().get(index);
                if (value != null) values.merge(source.times().get(index), value, Double::sum);
            }
        }
        List<Instant> times = values.keySet().stream().sorted().toList();
        List<Double> sortedValues = times.stream().map(values::get).toList();
        return List.of(new ChartSeriesResponse("sum", label, null, null, null, unit, "left", times, sortedValues));
    }

    static List<ChartSeriesResponse> appendComparison(List<ChartSeriesResponse> current,
                                                       List<ChartSeriesResponse> previous,
                                                       Comparison comparison) {
        if (comparison == null) return current;
        List<ChartSeriesResponse> result = new ArrayList<>(current);
        for (ChartSeriesResponse series : previous) {
            List<Instant> original = series.times();
            List<Instant> shifted = original.stream().map(time -> time.plusSeconds(comparison.offsetSeconds())).toList();
            result.add(new ChartSeriesResponse(series.key() + ":compare", series.label(),
                    null, null, null, series.unit(), "left", shifted, series.values(), true,
                    comparison.offsetSeconds(), original));
        }
        return List.copyOf(result);
    }

    record Comparison(String mode, String currentLabel, String previousLabel, long offsetSeconds,
                      QueryRanges.Range range, QueryRanges.Range currentRange) {
        String label() { return previousLabel; }
    }
}
