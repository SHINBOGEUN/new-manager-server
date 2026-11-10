package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;

public record AggregateDeviceValueResponse(
        Integer deviceId,
        String deviceName,
        String role,
        BigDecimal value,
        String displayValue,
        String displayUnit
) {
    public AggregateDeviceValueResponse(Integer deviceId, String deviceName, String role, BigDecimal value) {
        this(deviceId, deviceName, role, value, null, null);
    }
}
