package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PageWidgetPsychrometricCreateRequest(
        @NotBlank(message = "pageCode is required") String pageCode,
        @NotBlank(message = "name is required") @Size(max = 100) String name,
        Boolean enabled,
        Integer dataFreshnessMinutes,
        @NotEmpty(message = "temperatureSources is required") List<@Valid PageWidgetPsychrometricSourceRequest> temperatureSources,
        @NotEmpty(message = "humiditySources is required") List<@Valid PageWidgetPsychrometricSourceRequest> humiditySources,
        @Valid PageWidgetLayoutRequest layout,
        @jakarta.validation.constraints.Pattern(regexp = "classic|minimal|vivid", message = "visualStyle must be classic, minimal or vivid")
        String visualStyle,
        @jakarta.validation.constraints.Pattern(regexp = "^$|#[0-9a-fA-F]{6}", message = "visualColor must be #RRGGBB")
        String visualColor,
        java.util.Map<String, Object> clickConfig
) {
}
