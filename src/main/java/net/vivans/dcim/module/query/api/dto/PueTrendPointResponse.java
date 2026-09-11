package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PueTrendPointResponse(
        Instant time,
        BigDecimal value,
        BigDecimal totalPower,
        BigDecimal coolerPower
) {
}
