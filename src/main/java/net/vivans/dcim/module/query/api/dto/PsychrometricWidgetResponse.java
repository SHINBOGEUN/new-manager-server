package net.vivans.dcim.module.query.api.dto;

import java.time.Instant;
import java.util.List;

public record PsychrometricWidgetResponse(
        String title,
        List<Instant> timeLabels,
        List<PsychrometricDataResponse> data
) {
}
