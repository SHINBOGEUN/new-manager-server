package net.vivans.dcim.module.device.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RackWidgetService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final DeviceAssetRackService racks;
    private final DeviceMeasurementSourceCatalog catalog;
    private final PointQuery pointQuery;
    public record Source(Integer deviceId, String pointName, String protocol) {}
    public record Widget(String title, String type, int width, List<Source> sources, Integer rangeHours, Integer intervalMinutes, Integer gridX, Integer gridY, Integer layoutX, Integer layoutY, Integer layoutW, Integer layoutH) {
        public Widget(String title, String type, int width, List<Source> sources) { this(title, type, width, sources, null, null, null, null, null, null, null, null); }
        public Widget(String title, String type, int width, List<Source> sources, Integer rangeHours, Integer intervalMinutes) { this(title, type, width, sources, rangeHours, intervalMinutes, null, null, null, null, null, null); }
        public Widget(String title, String type, int width, List<Source> sources, Integer rangeHours, Integer intervalMinutes, Integer gridX, Integer gridY) { this(title, type, width, sources, rangeHours, intervalMinutes, gridX, gridY, null, null, null, null); }
    }
    public record WidgetValue(Source source, Double value, Instant time, List<Sample> samples) {}
    public record Sample(Instant time, Double value) {}
    private record WidgetDefinition(String type, List<Source> sources, Integer rangeHours, Integer intervalMinutes) {}
    public record RackWidgetDevice(Integer deviceId, String deviceName, String modelName, String deviceType, String pathCode, String pathName, boolean threePhaseMeasurements) {}

    @Transactional(readOnly = true)
    public List<RackWidgetDevice> devices(String code) {
        var available = sources(code);
        return jdbc.query("SELECT d.id,d.name,m.name,t.code,p.code,p.name FROM device_rack_placement r JOIN devices d ON d.id=r.device_id JOIN device_model m ON m.id=d.model_id JOIN common_code t ON t.id=m.device_type_id LEFT JOIN common_code p ON p.id=d.path_code_id WHERE r.rack_location_code=? ORDER BY d.name,d.id", (rs, n) -> {
            int id = rs.getInt(1);
            boolean hasThree = available.stream().filter(s -> s.deviceId().equals(id) && !s.ambiguous() && s.unit() != null && s.unit().equalsIgnoreCase("A"))
                    .map(s -> s.protocol().toLowerCase(Locale.ROOT)).distinct().anyMatch(protocol ->
                            List.of("L1_AMP", "L2_AMP", "L3_AMP").stream().allMatch(point -> available.stream().anyMatch(s -> s.deviceId().equals(id) && !s.ambiguous() && s.protocol().equalsIgnoreCase(protocol) && s.pointName().equalsIgnoreCase(point) && s.unit() != null && s.unit().equalsIgnoreCase("A"))));
            return new RackWidgetDevice(id, rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), hasThree);
        }, code);
    }

    private List<DeviceMeasurementSourceCatalog.Source> sources(String code) {
        var ids = racks.getRackLayout(code).placements().stream().map(p -> p.deviceId()).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        return ids.isEmpty() ? List.of() : catalog.availableSources(ids);
    }
    private static boolean allowed(Source s, List<DeviceMeasurementSourceCatalog.Source> allowed) {
        return s != null && s.deviceId() != null && s.pointName() != null && s.protocol() != null && allowed.stream().anyMatch(a -> !a.ambiguous() && a.deviceId().equals(s.deviceId()) && a.pointName().equals(s.pointName()) && a.protocol().equalsIgnoreCase(s.protocol()));
    }
    @Transactional(readOnly = true)
    public List<Widget> get(String code) {
        racks.getRackLayout(code);
        var rows = jdbc.query("SELECT widgets_json FROM rack_widget_config WHERE rack_location_code=?", (rs, n) -> rs.getString(1), code);
        if (rows.isEmpty()) return List.of();
        try { return mapper.readValue(rows.get(0), new TypeReference<List<Widget>>() {}); }
        catch (Exception e) { throw new IllegalStateException("랙 위젯 설정을 읽을 수 없습니다", e); }
    }
    @Transactional
    public List<Widget> save(String code, List<Widget> widgets) {
        validateForRack(code, widgets);
        try {
            String json = mapper.writeValueAsString(widgets);
            jdbc.update("INSERT INTO rack_widget_config(rack_location_code, widgets_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE widgets_json=VALUES(widgets_json)", code, json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalArgumentException("위젯 설정을 저장할 수 없습니다", e); }
        return widgets;
    }
    private List<DeviceMeasurementSourceCatalog.Source> validateForRack(String code, List<Widget> widgets) {
        var valid = sources(code);
        validateWidgets(widgets, valid);
        var deviceInfo = devices(code).stream().collect(java.util.stream.Collectors.toMap(RackWidgetDevice::deviceId, d -> d));
        validateRackAssignments(widgets, deviceInfo);
        return valid;
    }
    static void validateRackAssignments(List<Widget> widgets, Map<Integer, RackWidgetDevice> deviceInfo) {
        for (var w : widgets) {
            if (w.type().equals("PATH_BALANCE") || w.type().equals("POWER_TREND")) {
                for (int i = 0; i < 2; i++) { var device = deviceInfo.get(w.sources().get(i).deviceId());
                    if (device == null || !"PDU".equalsIgnoreCase(device.deviceType()) || !(i == 0 ? "A" : "B").equalsIgnoreCase(device.pathCode())) throw new IllegalArgumentException("A·B 경로에 등록된 PDU를 각각 선택하세요"); }
            } else if (w.type().equals("PHASE_BALANCE")) {
                for (int i = 0; i < w.sources().size(); i += 3) { var device = deviceInfo.get(w.sources().get(i).deviceId());
                    if (device == null || !"PDU".equalsIgnoreCase(device.deviceType()) || !device.threePhaseMeasurements()) throw new IllegalArgumentException("상별 전류 3개가 등록된 PDU를 선택하세요"); }
            }
        }
    }
    static void validateWidgets(List<Widget> widgets, List<DeviceMeasurementSourceCatalog.Source> valid) {
        if (widgets == null || widgets.size() > 24) throw new IllegalArgumentException("위젯은 최대 24개까지 설정할 수 있습니다");
        var occupied = new HashSet<String>();
        var names = new HashMap<String, String>();
        var definitions = new HashMap<WidgetDefinition, String>();
        for (var w : widgets) {
            if (w == null || w.title() == null || w.title().isBlank() || w.title().length() > 80 || w.type() == null || !Set.of("VALUE", "PIE", "TREND", "PATH_BALANCE", "PHASE_BALANCE", "POWER_TREND").contains(w.type()) || (w.width() != 1 && w.width() != 2) || w.sources() == null || w.sources().isEmpty() || w.sources().size() > 8 || (w.type().equals("VALUE") && w.sources().size() != 1) || w.sources().stream().anyMatch(s -> !allowed(s, valid)))
                throw new IllegalArgumentException("랙에 연결된 장비의 유효한 측정 항목을 선택하세요");
            String title = w.title().strip();
            String sameName = names.putIfAbsent(title.toLowerCase(Locale.ROOT), title);
            if (sameName != null) throw new IllegalArgumentException("이미 존재하는 위젯 이름입니다: " + sameName);
            var normalizedSources = new ArrayList<>(w.sources().stream().map(s -> new Source(s.deviceId(), s.pointName(), s.protocol().toLowerCase(Locale.ROOT))).toList());
            if (!w.type().equals("POWER_TREND") && !w.type().equals("PATH_BALANCE")) normalizedSources.sort(Comparator.comparing(Source::deviceId).thenComparing(Source::protocol).thenComparing(Source::pointName));
            var definition = new WidgetDefinition(w.type(), List.copyOf(normalizedSources), w.type().equals("POWER_TREND") ? (w.rangeHours() == null ? 24 : w.rangeHours()) : null, w.type().equals("POWER_TREND") ? (w.intervalMinutes() == null ? 15 : w.intervalMinutes()) : null);
            String sameDefinition = definitions.putIfAbsent(definition, title);
            if (sameDefinition != null) throw new IllegalArgumentException("이미 존재하는 위젯입니다: " + sameDefinition + " (동일한 종류와 측정 항목)");
            if ((w.gridX() == null) != (w.gridY() == null) || (w.gridX() != null && (w.gridX() < 0 || w.gridY() < 0 || w.gridY() >= 24 || w.gridX() + w.width() > 2))) throw new IllegalArgumentException("위젯 위치가 유효하지 않습니다");
            boolean hasLayout = w.layoutX() != null || w.layoutY() != null || w.layoutW() != null || w.layoutH() != null;
            if (hasLayout && (w.layoutX() == null || w.layoutY() == null || w.layoutW() == null || w.layoutH() == null || w.layoutX() < 0 || w.layoutY() < 0 || w.layoutW() < 2 || w.layoutH() < 2 || w.layoutX() + w.layoutW() > 12 || w.layoutY() + w.layoutH() > 192)) throw new IllegalArgumentException("위젯의 위치 또는 크기가 유효하지 않습니다");
            if (hasLayout || w.gridX() != null) {
                int x0 = hasLayout ? w.layoutX() : w.gridX() * 6, y0 = hasLayout ? w.layoutY() : w.gridY() * 4;
                int width = hasLayout ? w.layoutW() : w.width() * 6, height = hasLayout ? w.layoutH() : 4;
                for (int y = y0; y < y0 + height; y++) for (int x = x0; x < x0 + width; x++) if (!occupied.add(x + ":" + y)) throw new IllegalArgumentException("위젯 위치가 겹칩니다");
            }
            if ((w.type().equals("PATH_BALANCE") && w.sources().size() != 2) || (w.type().equals("PHASE_BALANCE") && w.sources().size() != 3 && w.sources().size() != 6) || (w.type().equals("POWER_TREND") && (w.sources().size() < 2 || w.sources().size() > 3)))
                throw new IllegalArgumentException("경로 또는 상별 측정 항목을 모두 선택하세요");
            if (Set.of("PATH_BALANCE", "PHASE_BALANCE", "POWER_TREND").contains(w.type())) {
                if (w.sources().stream().distinct().count() != w.sources().size()) throw new IllegalArgumentException("같은 측정 항목을 여러 위치에 선택할 수 없습니다");
                var units = w.sources().stream().map(s -> valid.stream().filter(a -> a.deviceId().equals(s.deviceId()) && a.pointName().equals(s.pointName()) && a.protocol().equalsIgnoreCase(s.protocol())).findFirst().orElseThrow().unit()).map(u -> u == null ? "" : u.trim().toLowerCase(Locale.ROOT)).distinct().toList();
                if (units.size() != 1 || units.get(0).isBlank()) throw new IllegalArgumentException("서로 같은 단위의 측정 항목을 선택하세요");
                if (w.type().equals("PHASE_BALANCE") ? !Set.of("a", "ma", "ka").contains(units.get(0)) : !Set.of("w", "kw", "mw").contains(units.get(0))) throw new IllegalArgumentException(w.type().equals("PHASE_BALANCE") ? "상 밸런스에는 전류(A) 항목을 선택하세요" : "경로 밸런스와 전력 추이에는 전력(W) 항목을 선택하세요");
                if (w.type().equals("PHASE_BALANCE")) {
                    for (int base = 0; base < w.sources().size(); base += 3) for (int i = 0; i < 3; i++) if (!w.sources().get(base+i).deviceId().equals(w.sources().get(base).deviceId()) || !w.sources().get(base+i).pointName().equalsIgnoreCase("L" + (i+1) + "_AMP")) throw new IllegalArgumentException("각 PDU의 L1_AMP, L2_AMP, L3_AMP를 선택하세요");
                    if (w.sources().size() == 6 && w.sources().get(0).deviceId().equals(w.sources().get(3).deviceId())) throw new IllegalArgumentException("서로 다른 PDU를 선택하세요");
                }
            }
            if (w.type().equals("POWER_TREND") && ((w.rangeHours() != null && !Set.of(1, 6, 24, 72, 168).contains(w.rangeHours())) || (w.intervalMinutes() != null && !Set.of(1, 5, 15, 60).contains(w.intervalMinutes())))) throw new IllegalArgumentException("유효한 기간과 집계 간격을 선택하세요");
        }
    }
    @Transactional(readOnly = true)
    public List<List<WidgetValue>> values(String code) {
        return valuesFor(get(code), sources(code));
    }
    @Transactional(readOnly = true)
    public List<List<WidgetValue>> preview(String code, List<Widget> widgets) {
        return valuesFor(widgets, validateForRack(code, widgets));
    }
    private List<List<WidgetValue>> valuesFor(List<Widget> widgets, List<DeviceMeasurementSourceCatalog.Source> valid) {
        Instant queryTime = Instant.now();
        return widgets.stream().map(w -> w.sources().stream().map(s -> {
            if (!allowed(s, valid)) return new WidgetValue(s, null, null, List.of());
            var last = pointQuery.findLast(List.of(s.deviceId()), List.of(s.pointName()), Duration.ofHours(24), s.protocol());
            var latest = last.isEmpty() ? null : last.get(0);
            List<Sample> samples = List.of();
            if (w.type().equals("TREND") || w.type().equals("POWER_TREND")) {
                Instant now = queryTime;
                int hours = w.type().equals("POWER_TREND") && w.rangeHours() != null ? w.rangeHours() : 24;
                int minutes = w.type().equals("POWER_TREND") && w.intervalMinutes() != null ? w.intervalMinutes() : 15;
                var raw = pointQuery.findMeanSeries(List.of(s.deviceId()), List.of(s.pointName()), now.minus(Duration.ofHours(hours)), now, s.protocol(), minutes);
                var buckets = new TreeMap<Long, double[]>(); long span = minutes * 60L;
                for (var p : raw) if (p.time() != null && p.value() != null && Double.isFinite(p.value())) { long t = Math.floorDiv(p.time().getEpochSecond(), span) * span; double[] aggregate = buckets.computeIfAbsent(t, ignored -> new double[2]); aggregate[0] += p.value(); aggregate[1]++; }
                var reduced = new ArrayList<Sample>();
                buckets.forEach((t, v) -> reduced.add(new Sample(Instant.ofEpochSecond(t), v[0] / v[1])));
                samples = reduced;
            }
            return new WidgetValue(s, latest == null ? null : latest.value(), latest == null ? null : latest.time(), samples);
        }).toList()).toList();
    }
}
