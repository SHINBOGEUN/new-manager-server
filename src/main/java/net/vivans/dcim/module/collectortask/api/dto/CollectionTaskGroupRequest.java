package net.vivans.dcim.module.collectortask.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CollectionTaskGroupRequest(
        @Schema(description = "그룹 이름", example = "1분 그룹")
        @NotBlank(message = "name must not be empty")
        String name,

        @Schema(description = "Spring cron 표현식", example = "0 */1 * * * *")
        @NotBlank(message = "cronExpression must not be empty")
        String cronExpression,

        @Schema(description = "이 주기로 수집할 장비 ID 목록", example = "[1, 2, 3]")
        List<Integer> deviceIds,

        @Schema(description = "그룹 활성 여부 (기본 true)", example = "true")
        Boolean active
) {
}
