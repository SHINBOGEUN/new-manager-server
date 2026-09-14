package net.vivans.dcim.module.operations.api.dto;

import java.time.Instant;

public record CollectionReconciliationResponse(
        Instant reconciledAt,
        int synchronizedGroupCount,
        int activePueDefinitionCount,
        String message
) {
}
