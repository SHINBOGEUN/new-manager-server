package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CalculatedMetricTrendPointResponse(
        Instant time,
        BigDecimal value,
        boolean compare,
        Long offsetSeconds,
        Instant originalTime,
        String compareLabel
) {
    public CalculatedMetricTrendPointResponse(Instant time, BigDecimal value) {
        this(time, value, false, null, null, null);
    }

    public CalculatedMetricTrendPointResponse(Instant time, BigDecimal value, boolean compare,
                                              Long offsetSeconds, Instant originalTime) {
        this(time, value, compare, offsetSeconds, originalTime, null);
    }
}
