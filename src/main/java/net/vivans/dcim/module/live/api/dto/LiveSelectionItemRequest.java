package net.vivans.dcim.module.live.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LiveSelectionItemRequest(
        @NotNull Integer deviceId,
        @NotEmpty List<String> pointNames
) {
}
