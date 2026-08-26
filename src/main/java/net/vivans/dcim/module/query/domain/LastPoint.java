package net.vivans.dcim.module.query.domain;

import java.time.Instant;

public record LastPoint(
        int deviceId,
        String pointName,
        double value,
        Instant time
) {
}
