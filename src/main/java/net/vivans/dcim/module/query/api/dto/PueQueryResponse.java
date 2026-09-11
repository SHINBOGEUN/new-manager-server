package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PueQueryResponse(
        BigDecimal value,
        BigDecimal totalPower,
        BigDecimal coolerPower,
        String unit,
        String rangePreset,
        Instant start,
        Instant end,
        boolean complete,
        String calculationStatus,
        List<Integer> missingDeviceIds,
        List<Integer> staleDeviceIds,
        List<PueDeviceValueResponse> devices,
        List<PueTrendPointResponse> trend,
        WidgetDataStatusResponse dataStatus
) {
}
