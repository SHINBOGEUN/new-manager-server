package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PageWidgetEnabledRequest(
        @NotNull(message = "enabled is required")
        @Schema(description = "UI 표시 여부", example = "true")
        Boolean enabled
) {
}
