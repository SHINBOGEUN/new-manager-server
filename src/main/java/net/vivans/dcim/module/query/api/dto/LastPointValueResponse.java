package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LastPointValueResponse(
        String pointName,
        String unit,
        BigDecimal value,
        Instant time,
        String displayValue,
        String displayUnit
) {
    public LastPointValueResponse(String pointName, String unit, BigDecimal value, Instant time) {
        this(pointName, unit, value, time, null, unit);
    }
}
