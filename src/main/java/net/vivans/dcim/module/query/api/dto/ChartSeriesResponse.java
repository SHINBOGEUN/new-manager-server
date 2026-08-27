package net.vivans.dcim.module.query.api.dto;

import java.time.Instant;
import java.util.List;

public record ChartSeriesResponse(
        String key,
        String label,
        Integer deviceId,
        String pointName,
        String locationNodeCode,
        List<Instant> times,
        List<Double> values
) {
}
