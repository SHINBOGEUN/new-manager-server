package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PageWidgetPsychrometricSourceRequest(
        @NotNull(message = "deviceId is required") Integer deviceId,
        @NotBlank(message = "pointName is required") @Size(max = 100) String pointName
) {
}
