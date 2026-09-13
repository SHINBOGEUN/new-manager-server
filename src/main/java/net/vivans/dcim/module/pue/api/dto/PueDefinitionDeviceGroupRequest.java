package net.vivans.dcim.module.pue.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PueDefinitionDeviceGroupRequest(
        @NotNull @Positive Integer deviceGroupId,
        @NotBlank String pointName
) {
}
