package net.vivans.dcim.module.query.application;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.vivans.dcim.module.query.domain.SeriesPoint;

final class PowerDemandCalculator {
    private PowerDemandCalculator() {}
    record Peak(Instant time, double value) {}

    // Influx aggregateWindow timestamps identify the end of each 15-minute window.
    static Peak peak(List<SeriesPoint> points, List<Integer> deviceIds, Instant start, Instant end) {
        Set<Integer> expected = new HashSet<>(deviceIds);
        Map<Instant, Map<Integer, Double>> windows = new TreeMap<>();
        Set<Instant> ambiguous = new HashSet<>();
        for (SeriesPoint point : points) {
            if (point.time() == null || point.value() == null || !Double.isFinite(point.value())
                    || !expected.contains(point.deviceId()) || point.time().isAfter(end)
                    || point.time().getEpochSecond() % 900 != 0 || point.time().getNano() != 0
                    || point.time().minusSeconds(900).isBefore(start)) continue;
            if (windows.computeIfAbsent(point.time(), ignored -> new HashMap<>()).put(point.deviceId(), point.value()) != null)
                ambiguous.add(point.time());
        }
        Peak result = null;
        for (var entry : windows.entrySet()) {
            if (ambiguous.contains(entry.getKey()) || entry.getValue().size() != expected.size()) continue;
            double total = entry.getValue().values().stream().mapToDouble(Double::doubleValue).sum();
            if (result == null || total > result.value()) result = new Peak(entry.getKey().minusSeconds(900), total);
        }
        return result;
    }
}
