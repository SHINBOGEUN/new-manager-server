package net.vivans.dcim.module.collectortask.infrastructure.collector;

import java.time.Instant;

public record CollectorJobResponse(
        String collectorJobId,
        Integer taskId,
        Integer groupId,
        Integer modelId,
        String protocol,
        String cronExpression,
        boolean enabled,
        int targetCount,
        Instant lastSuccessAt,
        Instant lastFailureAt,
        int consecutiveFailureCount,
        String lastFailureReason
) {
}
