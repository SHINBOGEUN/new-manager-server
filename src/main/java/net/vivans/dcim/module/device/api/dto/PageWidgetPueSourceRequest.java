package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PageWidgetPueSourceRequest(
        @NotNull @Positive Integer deviceId,
        @NotBlank String pointName
) {
}
