package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PageWidgetPowerDistributionUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        Boolean enabled,
        Integer dataFreshnessMinutes,
        @NotEmpty List<@Valid PageWidgetPowerDistributionGroupRequest> groups,
        @Valid PageWidgetLayoutRequest layout
) {
}
