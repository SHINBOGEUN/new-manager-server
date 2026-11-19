package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.query.api.dto.ChartSeriesResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.SeriesPoint;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

final class ComparisonSeriesSupport {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
    private ComparisonSeriesSupport() {}

    static ChartSeriesResponse sum(PointQuery pointQuery, List<Integer> deviceIds, List<String> pointNames,
                                   Map<String, String> typeBySource, String unit,
                                   QueryRanges.Range range, String window,
                                   Predicate<SeriesPoint> include, UnitNormalizer.Plan plan) {
        Map<Instant, Double> totals = new HashMap<>();
        Map<String, String> normalizedTypes = new HashMap<>();
        typeBySource.forEach((sourceKey, type) -> normalizedTypes.put(normalizeKey(sourceKey), type));
        List<SeriesPoint> averaged = pointQuery.findSeries(deviceIds, pointNames, range.start(), range.end(), window);
        for (SeriesPoint point : averaged) {
            if (!"ENERGY".equalsIgnoreCase(normalizedTypes.get(key(point.deviceId(), point.pointName())))
                    && include.test(point)) {
                totals.merge(bucket(point.time(), window), plan.value(point.deviceId(), point.pointName(), point.value()), Double::sum);
            }
        }

        Map<String, List<SeriesPoint>> readingsBySource = new LinkedHashMap<>();
        for (Map.Entry<String, String> source : typeBySource.entrySet()) {
            if (!"ENERGY".equalsIgnoreCase(source.getValue())) continue;
            int separator = source.getKey().indexOf('\u0000');
            int deviceId = Integer.parseInt(source.getKey().substring(0, separator));
            String pointName = source.getKey().substring(separator + 1);
            String normalizedKey = normalizeKey(source.getKey());
            readingsBySource.put(normalizedKey, new ArrayList<>());
            List<LastPoint> seed = pointQuery.findLastInRange(List.of(deviceId), List.of(pointName),
                    range.start().minus(Duration.ofDays(35)), range.start());
            seed.forEach(point -> readingsBySource.get(normalizedKey).add(
                    new SeriesPoint(point.deviceId(), point.pointName(), point.value(), point.time())));
        }
        List<SeriesPoint> rawEnergy = pointQuery.findRawSeries(deviceIds, pointNames, range.start(), range.end());
        for (SeriesPoint point : rawEnergy) {
            String sourceKey = key(point.deviceId(), point.pointName());
            if (readingsBySource.containsKey(sourceKey)) readingsBySource.get(sourceKey).add(point);
        }
        for (List<SeriesPoint> readings : readingsBySource.values()) {
            readings.sort(Comparator.comparing(SeriesPoint::time));
            SeriesPoint previous = null;
            for (SeriesPoint current : readings) {
                if (previous != null && !current.time().isBefore(range.start())
                        && !current.time().isAfter(range.end()) && include.test(current)) {
                    double increment = current.value() - previous.value();
                    if (increment >= 0) {
                        totals.merge(bucket(current.time(), window), plan.value(current.deviceId(), current.pointName(), increment), Double::sum);
                    }
                }
                previous = current;
            }
        }
        List<Instant> times = totals.keySet().stream().sorted().toList();
        return new ChartSeriesResponse("sum", "사용량 합계", null, null, null, unit, "left", times,
                times.stream().map(totals::get).toList());
    }

    private static long windowSeconds(String window) {
        return switch (window) {
            case "1m" -> 60;
            case "5m" -> 300;
            case "15m" -> 900;
            case "1h" -> 3600;
            case "1d" -> 86400;
            default -> throw new IllegalArgumentException("unsupported aggregate window");
        };
    }

    private static Instant bucket(Instant time, String window) {
        ZonedDateTime local = time.atZone(BUSINESS_ZONE);
        if ("1d".equals(window)) return local.toLocalDate().atStartOfDay(BUSINESS_ZONE).toInstant();
        if ("1h".equals(window)) return local.withMinute(0).withSecond(0).withNano(0).toInstant();
        int minutes = Math.toIntExact(windowSeconds(window) / 60);
        LocalDateTime localTime = local.toLocalDateTime().withMinute((local.getMinute() / minutes) * minutes)
                .withSecond(0).withNano(0);
        return localTime.atZone(BUSINESS_ZONE).toInstant();
    }

    private static String key(Integer deviceId, String pointName) {
        return deviceId + "\u0000" + pointName.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String normalizeKey(String sourceKey) {
        int separator = sourceKey.indexOf('\u0000');
        return separator < 0 ? sourceKey.toUpperCase(java.util.Locale.ROOT)
                : sourceKey.substring(0, separator + 1)
                + sourceKey.substring(separator + 1).trim().toUpperCase(java.util.Locale.ROOT);
    }
}
