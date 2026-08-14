package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DevicePageReplaceRequest(
        @Schema(description = "DEVICE_PAGE common_code ID 목록 (전체 교체)", example = "[10, 11]")
        @NotNull(message = "pageCodeIds is required")
        List<Integer> pageCodeIds
) {
}
