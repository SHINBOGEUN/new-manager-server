package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PageWidgetPowerDistributionGroupRequest(
        @NotBlank @Size(max = 100) String name,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "color must be #RRGGBB") String color,
        @NotEmpty List<@Valid PageWidgetPowerDistributionSourceRequest> sources
) {
}
