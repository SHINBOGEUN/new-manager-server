package net.vivans.dcim.module.devicegroup.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DeviceGroupRequest(
        @Schema(description = "그룹명", example = "IT 장비")
        @NotBlank(message = "name must not be blank")
        String name,

        @Schema(description = "그룹 설명", example = "서버실 IT 전력 대상")
        String description,

        @Schema(description = "사용 여부 (기본 true)", example = "true")
        Boolean enabled,

        @Schema(description = "포함할 장비 ID. 비우면 장비 없는 그룹", example = "[1, 2]")
        List<Integer> deviceIds
) {
}
