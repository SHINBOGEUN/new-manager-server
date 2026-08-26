package net.vivans.dcim.module.query.api.dto;

import java.time.Instant;

public record LastPointValueResponse(
        String pointName,
        String unit,
        Double value,
        Instant time
) {
}
