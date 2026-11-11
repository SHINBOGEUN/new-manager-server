package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PageWidgetTableColumnRequest(
        @NotBlank String pointName,
        @NotBlank @Size(max = 100) String label
) {
}
