package net.vivans.dcim.module.live.api.dto;

import java.time.Instant;
import java.util.List;

public record LiveSelectionResponse(
        List<LiveSelectionItemResponse> items,
        Instant expiresAt
) {
}
