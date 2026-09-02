package net.vivans.dcim.module.query.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AggregateWidgetResponse(
        Integer widgetId,
        String widgetName,
        String pageCode,
        String aggregatePreset,
        String rangePreset,
        Instant start,
        Instant end,
        /** 전체 합(또는 PUE 비율) */
        BigDecimal value,
        String unit,
        Integer contributingDevices,
        /** 장비별 값 (usage/power=기여분, pue=역할별 W) */
        List<AggregateDeviceValueResponse> devices
) {
}
