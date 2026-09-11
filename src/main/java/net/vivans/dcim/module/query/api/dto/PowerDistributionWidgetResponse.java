package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record PowerDistributionWidgetResponse(
        Integer widgetId,
        String title,
        BigDecimal totalPowerW,
        boolean complete,
        List<PowerDistributionGroupValueResponse> groups
) {
}
