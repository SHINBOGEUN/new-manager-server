package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PageWidgetPsychrometricUpdateRequest(
        @NotBlank(message = "name is required") @Size(max = 100) String name,
        Boolean enabled,
        Integer dataFreshnessMinutes,
        @NotEmpty(message = "temperatureSources is required") List<@Valid PageWidgetPsychrometricSourceRequest> temperatureSources,
        @NotEmpty(message = "humiditySources is required") List<@Valid PageWidgetPsychrometricSourceRequest> humiditySources,
        @Valid PageWidgetLayoutRequest layout
) {
}
