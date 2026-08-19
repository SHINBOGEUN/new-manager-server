package net.vivans.dcim.module.collectortask.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CollectionTaskUpdateRequest(
        @Schema(description = "Task 이름", example = "AP8959 SNMP 수집")
        @NotBlank(message = "name must not be empty")
        String name,

        @Schema(description = "활성 여부", example = "true")
        @NotNull(message = "active is required")
        Boolean active
) {
}
