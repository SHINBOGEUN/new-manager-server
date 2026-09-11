package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PageWidgetPowerDistributionCreateRequest(
        @NotBlank String pageCode,
        @NotBlank @Size(max = 100) String name,
        Boolean enabled,
        @NotEmpty List<@Valid PageWidgetPowerDistributionGroupRequest> groups,
        @Valid PageWidgetLayoutRequest layout
) {
}
