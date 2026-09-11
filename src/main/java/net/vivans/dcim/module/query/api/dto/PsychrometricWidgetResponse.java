package net.vivans.dcim.module.query.api.dto;

import java.time.Instant;
import java.util.List;

public record PsychrometricWidgetResponse(
        Integer widgetId,
        String title,
        List<Instant> timeLabels,
        List<PsychrometricDataResponse> data,
        WidgetDataStatusResponse dataStatus
) {
}
