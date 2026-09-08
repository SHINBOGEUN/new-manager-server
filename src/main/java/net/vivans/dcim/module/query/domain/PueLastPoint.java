package net.vivans.dcim.module.query.domain;

import java.time.Instant;

public record PueLastPoint(
        double value,
        Double totalPower,
        Double coolerPower,
        Instant time
) {
}
