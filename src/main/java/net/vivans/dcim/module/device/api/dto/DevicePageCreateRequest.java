package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record DevicePageCreateRequest(
        @Schema(description = "DEVICE_PAGE 그룹 common_code ID", example = "10")
        @NotNull(message = "pageCodeId is required")
        Integer pageCodeId
) {
}
