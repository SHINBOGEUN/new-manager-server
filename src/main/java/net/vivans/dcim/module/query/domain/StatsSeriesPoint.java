package net.vivans.dcim.module.query.domain;

import java.time.Instant;

public record StatsSeriesPoint(
        Integer deviceId,
        String pointName,
        Double min,
        Double max,
        Double avg,
        Instant time,
        String componentType,
        String componentIndex
) {
    public StatsSeriesPoint(Integer deviceId, String pointName, Double min, Double max, Double avg, Instant time) {
        this(deviceId, pointName, min, max, avg, time, null, null);
    }
}
