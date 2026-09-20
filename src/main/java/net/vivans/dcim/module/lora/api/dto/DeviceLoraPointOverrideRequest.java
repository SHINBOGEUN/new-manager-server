package net.vivans.dcim.module.lora.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceLoraPointOverrideRequest(
        @NotBlank String payloadField,
        @NotBlank String pointName,
        Integer dataPointTypeId,
        String unit,
        Double scale,
        String valueMap,
        Boolean enabled
) {
}
