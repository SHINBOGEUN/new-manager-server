package net.vivans.dcim.module.collectortask.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CollectionTaskCreateRequest(
        @Schema(description = "Task 이름", example = "AP8959 SNMP 수집")
        @NotBlank(message = "name must not be empty")
        String name,

        @Schema(description = "장비 모델 ID", example = "10")
        @NotNull(message = "modelId is required")
        Integer modelId,

        @Schema(description = "스크립트 타입 common_code ID (PROTOCOL_TYPE)", example = "9")
        @NotNull(message = "scriptTypeId is required")
        Integer scriptTypeId,

        @Schema(description = "활성 여부 (기본 true)", example = "true")
        Boolean active,

        @Schema(description = "주기 그룹")
        @Valid
        List<CollectionTaskGroupRequest> groups
) {
}
