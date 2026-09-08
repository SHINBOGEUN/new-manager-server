package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PueDeviceValueResponse(
        Integer deviceId,
        String deviceName,
        String role,
        String pointName,
        BigDecimal value,
        String unit,
        Instant time
) {
}
