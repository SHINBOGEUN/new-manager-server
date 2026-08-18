package net.vivans.dcim.module.collectortask.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CollectionTaskUpdateRequest(
        @Schema(description = "Task 이름", example = "전체 SNMP 수집")
        @NotBlank(message = "name must not be empty")
        String name,

        @Schema(description = "Spring cron 표현식", example = "0 */5 * * * *")
        @NotBlank(message = "cronExpression must not be empty")
        String cronExpression,

        @Schema(description = "스크립트 타입 common_code ID (PROTOCOL_TYPE 그룹)", example = "9")
        @NotNull(message = "scriptTypeId is required")
        Integer scriptTypeId,

        @Schema(description = "활성 여부", example = "false")
        @NotNull(message = "active is required")
        Boolean active
) {
}
