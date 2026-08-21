package net.vivans.dcim.module.collectortask.infrastructure.collector;

public record CollectorJobResponse(
        String collectorJobId,
        Integer taskId,
        Integer groupId,
        Integer modelId,
        String protocol,
        String cronExpression,
        boolean enabled,
        int targetCount
) {
}
