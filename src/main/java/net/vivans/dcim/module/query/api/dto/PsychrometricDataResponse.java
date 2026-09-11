package net.vivans.dcim.module.query.api.dto;

import java.util.List;

public record PsychrometricDataResponse(
        String label,
        List<Double> values,
        String unit
) {
}
