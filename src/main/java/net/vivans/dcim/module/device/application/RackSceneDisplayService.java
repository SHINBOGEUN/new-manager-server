package net.vivans.dcim.module.device.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RackSceneDisplayService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final DeviceAssetRackService racks;
    private final DeviceMeasurementSourceCatalog catalog;
    private final PointQuery pointQuery;
    public record Source(Integer deviceId, String pointName, String protocol) {}
    public record Sum(String title, List<Source> sources) {}
    public record Config(List<Source> labels, Source temperature, Double warningC, Double dangerC, Sum sum) {
        public Config(List<Source> labels, Source temperature, Double warningC, Double dangerC) { this(labels, temperature, warningC, dangerC, null); }
    }
    public record Reading(Source source, String deviceName, String unit, Double value, Instant time) {}
    public record SumReading(String title, String unit, Double value, List<Reading> sources) {}
    public record Display(List<Reading> labels, Reading temperature, double warningC, double dangerC, SumReading sum) {}
    private static Config empty() { return new Config(List.of(), null, 25.0, 30.0); }
    private List<DeviceMeasurementSourceCatalog.Source> available(String code) {
        var ids = racks.getRackLayout(code).placements().stream().map(p -> p.deviceId()).filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? List.of() : catalog.availableSources(ids);
    }
    private static DeviceMeasurementSourceCatalog.Source match(Source source, List<DeviceMeasurementSourceCatalog.Source> available) {
        if (source == null || source.deviceId() == null || source.pointName() == null || source.protocol() == null) return null;
        return available.stream().filter(a -> !a.ambiguous() && a.deviceId().equals(source.deviceId()) && a.pointName().equals(source.pointName()) && a.protocol().equalsIgnoreCase(source.protocol())).findFirst().orElse(null);
    }
    @Transactional(readOnly=true)
    public Config get(String code) {
        racks.getRackLayout(code);
        var rows=jdbc.query("SELECT display_json FROM rack_scene_display_config WHERE rack_location_code=?", (rs,n)->rs.getString(1), code);
        if (rows.isEmpty()) return empty();
        try { return mapper.readValue(rows.get(0), Config.class); } catch (Exception e) { throw new IllegalStateException("랙 도면 표시 설정을 읽을 수 없습니다", e); }
    }
    @Transactional
    public Config save(String code, Config config) {
        var valid=available(code);
        if (config == null || config.labels() == null || config.labels().size() > 6 || config.labels().stream().anyMatch(s -> match(s,valid)==null) || new HashSet<>(config.labels()).size() != config.labels().size())
            throw new IllegalArgumentException("랙에 연결된 표시 항목을 최대 6개까지 선택하세요");
        var temperature = config.temperature() == null ? null : match(config.temperature(), valid);
        if (config.temperature() != null && (temperature == null || !"TEMPERATURE".equalsIgnoreCase(temperature.dataPointType()) || !isCelsius(temperature.unit())))
            throw new IllegalArgumentException("바닥 색상에는 섭씨 온도 측정 항목을 선택하세요");
        double warning=config.warningC()==null?25:config.warningC(), danger=config.dangerC()==null?30:config.dangerC();
        if (!Double.isFinite(warning) || !Double.isFinite(danger) || warning < -100 || danger > 150 || warning >= danger) throw new IllegalArgumentException("온도 기준은 -100~150°C 범위에서 주의 온도가 위험 온도보다 낮아야 합니다");
        Sum sum = config.sum();
        if (sum != null) {
            if (sum.title() == null || sum.title().isBlank() || sum.title().length() > 40 || sum.sources() == null || sum.sources().size() < 2 || sum.sources().size() > 8 || new HashSet<>(sum.sources()).size() != sum.sources().size())
                throw new IllegalArgumentException("전력 합계 이름과 서로 다른 전력 항목 2~8개를 선택하세요");
            for (var source : sum.sources()) {
                var definition = match(source, valid);
                if (definition == null || powerFactor(definition.unit()) == null) throw new IllegalArgumentException("전력 합계에는 랙에 연결된 W 또는 kW 측정 항목을 선택하세요");
            }
            sum = new Sum(sum.title().trim(), List.copyOf(sum.sources()));
        }
        var normalized = new Config(List.copyOf(config.labels()), config.temperature(), warning, danger, sum);
        try { jdbc.update("INSERT INTO rack_scene_display_config(rack_location_code,display_json) VALUES (?,?) ON DUPLICATE KEY UPDATE display_json=VALUES(display_json)", code, mapper.writeValueAsString(normalized)); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalArgumentException("랙 도면 표시 설정을 저장할 수 없습니다",e); }
        return normalized;
    }
    static Double powerFactor(String unit) {
        if (unit == null) return null;
        return switch (unit.trim().toUpperCase(Locale.ROOT)) { case "W" -> 1.0; case "KW" -> 1000.0; case "MW" -> 1000000.0; default -> null; };
    }
    static boolean isCelsius(String unit) { if(unit==null)return false; return Set.of("C", "°C", "℃", "CELSIUS").contains(unit.trim().toUpperCase(Locale.ROOT)); }
    private Reading read(Source source, List<DeviceMeasurementSourceCatalog.Source> available) {
        var definition=match(source,available);
        if (definition == null) return new Reading(source, null, null, null, null);
        var points=pointQuery.findLast(List.of(source.deviceId()), List.of(source.pointName()), Duration.ofHours(24), source.protocol());
        var point=points.isEmpty()?null:points.get(0);
        return new Reading(source, definition.deviceName(), definition.unit(), point == null || !Double.isFinite(point.value()) ? null : point.value(), point == null ? null : point.time());
    }
    @Transactional(readOnly=true)
    public Display values(String code) {
        var config=get(code); var available=available(code);
        var labels=config.labels()==null?List.<Reading>of():config.labels().stream().map(s->read(s,available)).toList();
        var definition = match(config.temperature(), available);
        var temperature = definition != null && "TEMPERATURE".equalsIgnoreCase(definition.dataPointType()) && isCelsius(definition.unit())
                ? read(config.temperature(), available) : null;
        SumReading sum = null;
        if (config.sum() != null && config.sum().sources() != null) {
            var readings = config.sum().sources().stream().map(s -> read(s, available)).toList();
            boolean complete = readings.size() >= 2 && readings.stream().allMatch(r -> r.value() != null && powerFactor(r.unit()) != null);
            double total = complete ? readings.stream().mapToDouble(r -> r.value() * powerFactor(r.unit())).sum() : 0;
            sum = new SumReading(config.sum().title(), "W", complete && Double.isFinite(total) ? total : null, readings);
        }
        return new Display(labels, temperature, config.warningC()==null?25:config.warningC(), config.dangerC()==null?30:config.dangerC(), sum);
    }
}
