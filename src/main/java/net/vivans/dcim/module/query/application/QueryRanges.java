package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

final class QueryRanges {

    private QueryRanges() {
    }

    static Range resolve(PageWidgetChartRangePreset preset) {
        Instant now = Instant.now();
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        return switch (preset) {
            case last_24h -> new Range(now.minus(24, ChronoUnit.HOURS), now);
            case today -> new Range(todayUtc.atStartOfDay().toInstant(ZoneOffset.UTC), now);
            case yesterday -> {
                LocalDate yesterday = todayUtc.minusDays(1);
                yield new Range(
                        yesterday.atStartOfDay().toInstant(ZoneOffset.UTC),
                        todayUtc.atStartOfDay().toInstant(ZoneOffset.UTC)
                );
            }
            case last_7d -> new Range(now.minus(7, ChronoUnit.DAYS), now);
            case this_month -> new Range(
                    todayUtc.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                    now
            );
            case last_month -> {
                LocalDate firstOfThisMonth = todayUtc.withDayOfMonth(1);
                LocalDate firstOfLastMonth = firstOfThisMonth.minusMonths(1);
                yield new Range(
                        firstOfLastMonth.atStartOfDay().toInstant(ZoneOffset.UTC),
                        firstOfThisMonth.atStartOfDay().toInstant(ZoneOffset.UTC)
                );
            }
        };
    }

    /** weighted_avg 시계열 윈도우 */
    static String defaultWindow(PageWidgetChartRangePreset preset) {
        return switch (preset) {
            case last_24h, today, yesterday -> "5m";
            case last_7d, this_month, last_month -> "1h";
        };
    }

    record Range(Instant start, Instant end) {
    }
}
