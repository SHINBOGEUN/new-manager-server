package net.vivans.dcim.module.query.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PueSourceRequest(
        @NotNull(message = "deviceId is required")
        @Positive(message = "deviceId must be positive")
        Integer deviceId,

        @NotBlank(message = "pointName is required")
        @Size(max = 100, message = "pointName must be at most 100 characters")
        String pointName
) {
}
