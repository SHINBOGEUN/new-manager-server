package net.vivans.dcim.module.query.domain;

import java.time.Instant;

public record SeriesPoint(
        Integer deviceId,
        String pointName,
        Double value,
        Instant time
) {
}
