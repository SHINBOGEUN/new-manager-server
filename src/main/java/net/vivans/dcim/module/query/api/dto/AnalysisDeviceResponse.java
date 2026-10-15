package net.vivans.dcim.module.query.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AnalysisDeviceResponse(
        Integer deviceId,
        String displayName,
        String type,
        String aggregate,
        LocalDate startDate,
        LocalDate endDate,
        List<SensorGroup> sensors
) {
    public record SensorGroup(String category, List<UnitGroup> units) {
    }

    public record UnitGroup(String unit, List<Field> fields) {
    }

    public record Field(
            String pointName,
            String displayName,
            List<RawValue> raws,
            List<StatsValue> stats,
            String componentType,
            String componentIndex
    ) {
        public Field(String pointName, String displayName, List<RawValue> raws, List<StatsValue> stats) {
            this(pointName, displayName, raws, stats, null, null);
        }
    }

    public record RawValue(Instant time, Double value) {
    }

    public record StatsValue(Instant time, Double min, Double max, Double avg) {
    }
}
