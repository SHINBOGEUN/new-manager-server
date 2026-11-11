package net.vivans.dcim.module.devicemodel.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceModelDetailsUpdateRequest(
        @NotBlank String name,
        @NotBlank String manufacturer,
        @NotNull Integer deviceTypeId,
        String description,
        String phaseNotation
) {
    public DeviceModelDetailsUpdateRequest(String name, String manufacturer, Integer deviceTypeId, String description) {
        this(name, manufacturer, deviceTypeId, description, null);
    }
}
