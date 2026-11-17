package net.vivans.dcim.module.query.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.application.DeviceMeasurementSourceCatalog;
import net.vivans.dcim.module.query.domain.SeriesPoint;
import org.springframework.stereotype.Component;
import java.util.*;

/** Resolves conversion from configured UNIT metadata, never by unit name. */
@Component
@RequiredArgsConstructor
public class UnitNormalizer {
    private final CommonCodeRepository codes;
    private final DeviceMeasurementSourceCatalog sources;
    public record Conversion(String unit, double factor) {}
    public record Plan(Map<String, Conversion> conversions) {
        public Conversion forSource(int deviceId, String pointName) {
            return conversions.getOrDefault(key(deviceId, pointName), new Conversion(null, 1));
        }
        public double value(int deviceId, String pointName, double value) { return value * forSource(deviceId, pointName).factor(); }
        public SeriesPoint point(SeriesPoint p) {
            return new SeriesPoint(p.deviceId(), p.pointName(), p.value() == null ? null : value(p.deviceId(), p.pointName(), p.value()), p.time(), p.componentType(), p.componentIndex());
        }
        public List<String> units() { return conversions.values().stream().map(Conversion::unit).filter(Objects::nonNull).distinct().toList(); }
    }
    public static String key(int id, String point) { return id + "\u0000" + point.trim().toUpperCase(java.util.Locale.ROOT); }
    public Plan plan(Set<Integer> ids, Collection<String> points) {
        Map<String, Conversion> configured = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        codes.findAll().stream().filter(c -> "UNIT".equals(c.getCodeGroup().getGroupKey())).forEach(c -> {
            String name = c.getName().trim();
            Conversion value = c.getBaseUnit() != null && c.getUnitFactor() != null && c.getUnitFactor().signum() > 0
                    ? new Conversion(c.getBaseUnit().getName().trim(), c.getUnitFactor().doubleValue()) : new Conversion(name, 1);
            Conversion old = configured.putIfAbsent(name, value);
            if (old != null && !old.equals(value)) ambiguous.add(name);
        });
        Map<String, Conversion> result = new LinkedHashMap<>();
        for (var source : sources.availableSources(ids)) {
            if (source.ambiguous() || !points.contains(source.pointName())) continue;
            String raw = source.unit() == null || source.unit().isBlank() ? null : source.unit().trim();
            Conversion conversion = raw == null ? new Conversion(null, 1) : ambiguous.contains(raw)
                    ? new Conversion(raw, 1) : configured.getOrDefault(raw, new Conversion(raw, 1));
            result.putIfAbsent(key(source.deviceId(), source.pointName()), conversion);
        }
        return new Plan(result);
    }
}
