package net.vivans.dcim.module.query.api.dto;

import java.time.Instant;
import java.util.List;

public record ChartSeriesResponse(
        String key,
        String label,
        Integer deviceId,
        String pointName,
        String locationNodeCode,
        String unit,
        String axis,
        List<Instant> times,
        List<Double> values,
        boolean compare,
        Long offsetSeconds,
        List<Instant> originalTimes
) {
    public ChartSeriesResponse(String key, String label, Integer deviceId, String pointName,
                               String locationNodeCode, String unit, String axis,
                               List<Instant> times, List<Double> values) {
        this(key, label, deviceId, pointName, locationNodeCode, unit, axis,
                times, values, false, null, List.of());
    }
}
