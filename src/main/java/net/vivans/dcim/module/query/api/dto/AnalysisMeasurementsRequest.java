package net.vivans.dcim.module.query.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record AnalysisMeasurementsRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotEmpty List<@Valid Target> targets
) {
    public record Target(
            @NotNull Integer deviceId,
            @NotEmpty List<@NotBlank String> pointNames,
            List<@NotBlank String> componentIndices,
            List<@Valid ComponentSelection> componentSelections
    ) {
        public Target(Integer deviceId, List<String> pointNames) { this(deviceId, pointNames, null, null); }
        public Target(Integer deviceId, List<String> pointNames, List<String> componentIndices) { this(deviceId, pointNames, componentIndices, null); }
    }
    public record ComponentSelection(@NotBlank String pointName, @NotBlank String componentType, @NotBlank String componentIndex) {}
}
