package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PageWidgetPueUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        Boolean enabled,
        String rangePreset,
        Integer freshnessMinutes,
        @NotEmpty List<@Valid PageWidgetPueSourceRequest> totalSources,
        @NotEmpty List<@Valid PageWidgetPueSourceRequest> coolerSources,
        @Valid PageWidgetLayoutRequest layout
) {
}
