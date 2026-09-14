package net.vivans.dcim.module.query.api.dto;

import java.time.Instant;
import java.util.List;

public record CollectionStatusResponse(
        Instant generatedAt,
        CollectionStatusSummary summary,
        List<CollectionDeviceStatusResponse> devices
) {
    public record CollectionStatusSummary(
            int totalCount, int normalCount, int staleCount, int missingCount,
            int stoppedCount, int unregisteredCount, int disabledCount
    ) {}
}
