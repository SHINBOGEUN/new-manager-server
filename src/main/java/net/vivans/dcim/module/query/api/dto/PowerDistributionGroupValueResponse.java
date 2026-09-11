package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record PowerDistributionGroupValueResponse(
        String name,
        String color,
        BigDecimal powerW,
        BigDecimal ratio,
        boolean complete,
        List<PowerDistributionSourceValueResponse> sources
) {
}
