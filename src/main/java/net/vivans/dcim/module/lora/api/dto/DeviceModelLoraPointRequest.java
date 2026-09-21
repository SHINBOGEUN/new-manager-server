package net.vivans.dcim.module.lora.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceModelLoraPointRequest(
        @NotBlank String payloadField,
        @NotBlank String pointName,
        Integer dataPointTypeId,
        String unit,
        Double scale,
        String valueMap,
        Boolean enabled
) {
}
