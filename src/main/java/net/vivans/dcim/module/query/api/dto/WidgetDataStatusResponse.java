package net.vivans.dcim.module.query.api.dto;

import java.time.Instant;

/** 위젯 원천 데이터의 공통 수집 상태. */
public record WidgetDataStatusResponse(
        String status,
        Instant latestCollectedAt,
        int freshnessMinutes,
        int expectedSourceCount,
        int availableSourceCount,
        int missingSourceCount,
        int staleSourceCount
) {
}
