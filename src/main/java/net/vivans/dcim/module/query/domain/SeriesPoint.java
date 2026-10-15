package net.vivans.dcim.module.query.domain;

import java.time.Instant;

public record SeriesPoint(
        Integer deviceId,
        String pointName,
        Double value,
        Instant time,
        String componentType,
        String componentIndex
) {
    public SeriesPoint(Integer deviceId, String pointName, Double value, Instant time) {
        this(deviceId, pointName, value, time, null, null);
    }
}
