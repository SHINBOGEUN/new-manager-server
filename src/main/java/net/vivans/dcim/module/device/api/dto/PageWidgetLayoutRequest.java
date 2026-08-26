package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PageWidgetLayoutRequest(
        @Schema(description = "그리드 X", example = "0")
        @NotNull(message = "gridX is required")
        @Min(value = 0, message = "gridX must be >= 0")
        Integer gridX,

        @Schema(description = "그리드 Y", example = "0")
        @NotNull(message = "gridY is required")
        @Min(value = 0, message = "gridY must be >= 0")
        Integer gridY,

        @Schema(description = "가로 칸 수", example = "2")
        @NotNull(message = "w is required")
        @Min(value = 1, message = "w must be >= 1")
        Integer w,

        @Schema(description = "세로 칸 수", example = "1")
        @NotNull(message = "h is required")
        @Min(value = 1, message = "h must be >= 1")
        Integer h
) {
}
