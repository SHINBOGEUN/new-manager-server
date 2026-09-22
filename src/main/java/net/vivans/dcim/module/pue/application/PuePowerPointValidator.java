package net.vivans.dcim.module.pue.application;

import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 현재 PUE Collector가 사용하는 SNMP POWER 포인트 검증 정책을 한 곳에 모은다. */
public final class PuePowerPointValidator {

    private final DeviceModelSnmpPointRepository pointRepository;

    public PuePowerPointValidator(DeviceModelSnmpPointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    public ValidationResult validateQuerySources(List<Source> sources) {
        return validate(sources, ValidationContext.QUERY);
    }

    public void validateWidgetSources(List<Source> sources) {
        validate(sources, ValidationContext.WIDGET);
    }

    public void validateDefinitionSources(List<Source> sources) {
        validate(sources, ValidationContext.DEFINITION);
    }

    private ValidationResult validate(List<Source> sources, ValidationContext context) {
        if (sources == null || sources.isEmpty()) {
            return new ValidationResult(null, Map.of());
        }

        Set<Integer> modelIds = new LinkedHashSet<>();
        for (Source source : sources) {
            if (source.device() == null || source.device().getId() == null) {
                throw new IllegalArgumentException("PUE source device is required");
            }
            if (!source.device().isEnabled()) {
                throw new IllegalArgumentException("PUE source device is disabled: " + source.device().getId());
            }
            modelIds.add(source.device().getDeviceModel().getId());
        }

        Map<String, List<DeviceModelSnmpPoint>> catalog = new HashMap<>();
        for (DeviceModelSnmpPoint point : pointRepository.findAllEnabledByDeviceModelIds(modelIds)) {
            catalog.computeIfAbsent(
                    key(point.getModelProtocol().getDeviceModel().getId(), point.getName()),
                    ignored -> new ArrayList<>()).add(point);
        }

        String commonUnit = null;
        Map<String, String> sourceUnits = new HashMap<>();
        for (Source source : sources) {
            List<DeviceModelSnmpPoint> matches = catalog.getOrDefault(
                    key(source.device().getDeviceModel().getId(), source.pointName()), List.of());
            DeviceModelSnmpPoint point = selectPoint(matches, context);
            if (point == null || point.getDataPointType() == null
                    || !"POWER".equalsIgnoreCase(point.getDataPointType().getCode())) {
                throw invalidPowerPoint(source, context, point == null);
            }

            String unit = requireUnit(point.getUnit(), source, context);
            if (context == ValidationContext.DEFINITION && !"W".equalsIgnoreCase(unit)) {
                throw new IllegalArgumentException("PUE group point unit must be W: device "
                        + source.device().getId() + ", point " + source.pointName());
            }
            if (commonUnit == null) {
                commonUnit = unit;
            } else if (!commonUnit.equalsIgnoreCase(unit)) {
                if (context == ValidationContext.WIDGET) {
                    throw new IllegalArgumentException("PUE source points must use the same unit");
                }
                throw new IllegalArgumentException("PUE source points must use the same unit: "
                        + commonUnit + ", " + unit);
            }
            sourceUnits.put(key(source.device().getDeviceModel().getId(), source.pointName()), unit);
        }
        return new ValidationResult(commonUnit, sourceUnits);
    }

    private static DeviceModelSnmpPoint selectPoint(
            List<DeviceModelSnmpPoint> matches,
            ValidationContext context
    ) {
        if (matches.isEmpty()) {
            return null;
        }
        return context == ValidationContext.QUERY
                ? matches.get(0)
                : matches.get(matches.size() - 1);
    }

    private static IllegalArgumentException invalidPowerPoint(
            Source source,
            ValidationContext context,
            boolean missing
    ) {
        if (context == ValidationContext.DEFINITION) {
            return new IllegalArgumentException("PUE group point must be an enabled POWER point: device "
                    + source.device().getId() + ", point " + source.pointName());
        }
        if (context == ValidationContext.WIDGET) {
            return new IllegalArgumentException("PUE source must be an enabled POWER point: device "
                    + source.device().getId() + ", point " + source.pointName());
        }
        if (missing) {
            return new IllegalArgumentException("POWER point not found for device "
                    + source.device().getId() + ": " + source.pointName());
        }
        return new IllegalArgumentException("PUE source point must have DATA_POINT_TYPE=POWER: device "
                + source.device().getId() + ", point " + source.pointName());
    }

    private static String requireUnit(String unit, Source source, ValidationContext context) {
        if (unit != null && !unit.isBlank()) {
            return unit.trim();
        }
        if (context == ValidationContext.DEFINITION) {
            throw new IllegalArgumentException("PUE group point unit must be W: device "
                    + source.device().getId() + ", point " + source.pointName());
        }
        if (context == ValidationContext.WIDGET) {
            throw new IllegalArgumentException("PUE source unit is required: " + source.pointName());
        }
        throw new IllegalArgumentException("PUE source point unit is required: device "
                + source.device().getId() + ", point " + source.pointName());
    }

    private static String key(Integer modelId, String pointName) {
        if (pointName == null || pointName.isBlank()) {
            throw new IllegalArgumentException("PUE source pointName is required");
        }
        return modelId + "|" + pointName.trim().toUpperCase(Locale.ROOT);
    }

    private enum ValidationContext {
        QUERY,
        WIDGET,
        DEFINITION
    }

    public record Source(Device device, String pointName) {
    }

    public record ValidationResult(String commonUnit, Map<String, String> sourceUnits) {

        public ValidationResult {
            sourceUnits = Map.copyOf(sourceUnits);
        }

        public String unitFor(Source source) {
            return sourceUnits.get(key(source.device().getDeviceModel().getId(), source.pointName()));
        }
    }
}
