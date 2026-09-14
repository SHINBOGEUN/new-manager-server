package net.vivans.dcim.module.operations.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CollectionOperationsHealthResponse(
        Instant checkedAt,
        List<Component> components,
        CollectorSyncSummary collectorSync
) {

    public record Component(
            String code,
            String name,
            String status,
            String target,
            String message,
            Map<String, Object> details
    ) {
    }

    public record CollectorSyncSummary(
            boolean syncEnabled,
            int activeTaskGroups,
            int groupsWithCollectorJob,
            int groupsMissingCollectorJob,
            int groupsMissingSpec,
            Integer collectorReportedJobs,
            String collectorInstanceId
    ) {
    }
}
