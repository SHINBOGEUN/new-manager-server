package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceCreateRequest(
        @Schema(description = "장비 모델 ID", example = "1")
        @NotNull(message = "modelId is required")
        Integer modelId,

        @Schema(description = "위치 노드 code (미지정 시 UNASSIGNED)", example = "UNASSIGNED")
        @NotBlank(message = "locationNodeCode must not be empty")
        String locationNodeCode,

        @Schema(description = "현장 표시명", example = "PDU-좌")
        @NotBlank(message = "name must not be empty")
        String name,

        @Schema(description = "설명", example = "TEST PDU 입니다.")
        String description,

        @Schema(description = "사용 여부 (기본 true)", example = "true")
        Boolean enabled,

        @Schema(description = "Path 코드 ID (LOCATION_PATH 그룹, 선택). PDU 전원 피드 / 차트 by_path", example = "10")
        Integer pathCodeId
) {
}
