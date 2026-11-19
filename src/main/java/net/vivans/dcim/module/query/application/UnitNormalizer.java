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
        public double value(int deviceId, String pointName, double value) {
            Conversion conversion = conversions.get(key(deviceId, pointName));
            if (conversion == null) throw new IllegalArgumentException("측정항목의 단위 정보를 확인할 수 없습니다: " + deviceId + " / " + pointName);
            return value * conversion.factor();
        }
        public SeriesPoint point(SeriesPoint p) {
            return new SeriesPoint(p.deviceId(), p.pointName(), p.value() == null ? null : value(p.deviceId(), p.pointName(), p.value()), p.time(), p.componentType(), p.componentIndex());
        }
        public boolean converted() { return conversions.values().stream().anyMatch(item -> Double.compare(item.factor(), 1d) != 0); }
        public List<String> units() { return conversions.values().stream().map(Conversion::unit).filter(Objects::nonNull).distinct().toList(); }
    }
    public static String key(int id, String point) { return id + "\u0000" + point.trim().toUpperCase(java.util.Locale.ROOT); }
    public Plan plan(Set<Integer> ids, Collection<String> points) {
        Map<String, Conversion> configured = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        Set<String> invalid = new HashSet<>();
        codes.findAll().stream().filter(c -> "UNIT".equals(c.getCodeGroup().getGroupKey())).forEach(c -> {
            String name = c.getName().trim();
            var base = c.getBaseUnit();
            boolean incomplete = (base == null) != (c.getUnitFactor() == null);
            boolean badBase = base != null && (c.getUnitFactor() == null || !"UNIT".equals(base.getCodeGroup().getGroupKey()) || c.getUnitFactor().signum() <= 0
                    || base.getBaseUnit() == null || !Objects.equals(base.getId(), base.getBaseUnit().getId())
                    || base.getUnitFactor() == null || base.getUnitFactor().compareTo(java.math.BigDecimal.ONE) != 0);
            if (incomplete || badBase) { invalid.add(name); return; }
            Conversion value = base != null ? new Conversion(base.getName().trim(), c.getUnitFactor().doubleValue()) : new Conversion(name, 1);
            Conversion old = configured.putIfAbsent(name, value);
            if (old != null && !old.equals(value)) ambiguous.add(name);
        });
        Map<String, Conversion> result = new LinkedHashMap<>();
        for (var source : sources.availableSources(ids)) {
            if (source.ambiguous() || points.stream().noneMatch(point -> point.equalsIgnoreCase(source.pointName()))) continue;
            String raw = source.unit() == null || source.unit().isBlank() ? null : source.unit().trim();
            if (raw != null && invalid.contains(raw)) throw new IllegalStateException("단위 환산 설정이 올바르지 않습니다: " + raw);
            if (raw != null && ambiguous.contains(raw)) throw new IllegalStateException("같은 단위 이름에 서로 다른 환산 설정이 있습니다: " + raw);
            Conversion conversion = raw == null ? new Conversion(null, 1) : configured.getOrDefault(raw, new Conversion(raw, 1));
            result.putIfAbsent(key(source.deviceId(), source.pointName()), conversion);
        }
        return new Plan(result);
    }
}
