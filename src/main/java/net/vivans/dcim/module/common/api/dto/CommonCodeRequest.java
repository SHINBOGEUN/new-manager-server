package net.vivans.dcim.module.common.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommonCodeRequest(
        @Schema(description = "코드 그룹 ID", example = "1")
        @NotNull(message = "groupId must not be null")
        Integer groupId,

        @Schema(description = "코드 값", example = "ups")
        @NotBlank(message = "code must not be empty")
        String code,

        @Schema(description = "코드 표시명", example = "UPS")
        @NotBlank(message = "name must not be empty")
        String name,

        @Schema(description = "정렬 순서", example = "1")
        Integer sortOrder,

        @Schema(description = "임계값 포인트 허용 여부 (DATA_POINT_TYPE에서만 사용)", example = "true")
        Boolean thresholdSupported
) {
    public CommonCodeRequest(Integer groupId, String code, String name, Integer sortOrder) {
        this(groupId, code, name, sortOrder, null);
    }
}
