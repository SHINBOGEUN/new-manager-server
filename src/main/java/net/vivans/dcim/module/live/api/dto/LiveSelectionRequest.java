package net.vivans.dcim.module.live.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LiveSelectionRequest(
        @NotNull @Valid List<LiveSelectionItemRequest> items
) {
}
