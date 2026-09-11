package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PowerDistributionSourceValueResponse(
        Integer deviceId,
        String deviceName,
        String pointName,
        BigDecimal powerW,
        Instant time
) {
}
