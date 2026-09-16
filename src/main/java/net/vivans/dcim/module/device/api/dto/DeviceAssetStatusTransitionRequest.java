package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceAssetStatusTransitionRequest(
        @Schema(description = "변경할 ASSET_STATUS 공통코드 ID", example = "20")
        @NotNull(message = "statusId is required") Integer statusId,
        @Schema(description = "상태 변경 사유", example = "장비 교체로 폐기 처리")
        @NotBlank(message = "reason is required") String reason
) {}
