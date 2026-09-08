package net.vivans.dcim.module.query.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PueQueryRequest(
        @NotEmpty(message = "totalSources is required")
        @Size(max = 200, message = "totalSources must contain at most 200 items")
        List<@Valid PueSourceRequest> totalSources,

        @NotEmpty(message = "coolerSources is required")
        @Size(max = 200, message = "coolerSources must contain at most 200 items")
        List<@Valid PueSourceRequest> coolerSources,

        String rangePreset
) {
}
