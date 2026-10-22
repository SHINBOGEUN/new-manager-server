package net.vivans.dcim.module.alarm.api;

import java.util.List;

public record AlarmSummaryResponse(
        long openCount,
        long warningCount,
        long criticalCount,
        List<AlarmEventResponse> latestOpen
) {}
