package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PageWidgetPowerDistributionSourceRequest(
        @NotNull Integer deviceId,
        @NotBlank String pointName
) {
}
