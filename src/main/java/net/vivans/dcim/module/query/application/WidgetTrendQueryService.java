package net.vivans.dcim.module.query.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.device.domain.model.PageWidgetDeviceRole;
import net.vivans.dcim.module.device.domain.model.PageWidgetPowerDistributionGroup;
import net.vivans.dcim.module.device.domain.model.PageWidgetPowerDistributionSource;
import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometricSource;
import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometricSourceRole;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.query.api.dto.ChartSeriesResponse;
import net.vivans.dcim.module.query.api.dto.ChartWidgetResponse;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.SeriesPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** chart 외 데이터 위젯도 동일한 2축 트렌드 화면에서 조회한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WidgetTrendQueryService {

    private final PageWidgetRepository pageWidgetRepository;
    private final PointQuery pointQuery;
    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;

    public ChartWidgetResponse getTrend(Integer widgetId, String rangePresetRaw, String window) {
        PageWidget widget = PageWidgetFinder.findRequired(pageWidgetRepository, widgetId);
        if (!widget.isEnabled()) throw new IllegalArgumentException("widget is disabled");
        PageWidgetChartRangePreset preset = PageWidgetChartRangePreset.from(rangePresetRaw);
        String resolvedWindow = resolveWindow(window);
        QueryRanges.Range range = QueryRanges.resolve(preset);

        return switch (widget.getQueryKind()) {
            case last, aggregate -> standardTrend(widget, preset, resolvedWindow, range);
            case psychrometric -> psychrometricTrend(widget, preset, resolvedWindow, range);
            case power_distribution -> powerDistributionTrend(widget, preset, resolvedWindow, range);
            case chart -> throw new IllegalArgumentException("chart widget must use /query/chart");
            case pue -> throw new IllegalArgumentException("pue widget must use /query/pue");
            case count -> throw new IllegalArgumentException("count widget has no time-series data");
        };
    }

    private ChartWidgetResponse standardTrend(
            PageWidget widget, PageWidgetChartRangePreset preset, String window, QueryRanges.Range range
    ) {
        List<Source> sources = standardSources(widget);
        List<ChartSeriesResponse> series = toSourceSeries(sources, range, window);
        List<String> units = units(sources);
        validateUnitCount(units);
        return response(widget, preset, window, range, "devices", "per_device", units, series);
    }

    private ChartWidgetResponse psychrometricTrend(
            PageWidget widget, PageWidgetChartRangePreset preset, String window, QueryRanges.Range range
    ) {
        List<PageWidgetPsychrometricSource> sources = new ArrayList<>(widget.getPsychrometric().getSources());
        List<Integer> deviceIds = sources.stream().map(source -> source.getDevice().getId()).distinct().toList();
        List<String> pointNames = sources.stream().map(PageWidgetPsychrometricSource::getPointName).distinct().toList();
        Map<String, PageWidgetPsychrometricSourceRole> roleBySource = new HashMap<>();
        for (PageWidgetPsychrometricSource source : sources) {
            roleBySource.put(key(source.getDevice().getId(), source.getPointName()), source.getRole());
        }
        Map<Instant, List<Double>> temperatures = new LinkedHashMap<>();
        Map<Instant, List<Double>> humidities = new LinkedHashMap<>();
        for (SeriesPoint point : pointQuery.findSeries(deviceIds, pointNames, range.start(), range.end(), window)) {
            PageWidgetPsychrometricSourceRole role = roleBySource.get(key(point.deviceId(), point.pointName()));
            if (role == PageWidgetPsychrometricSourceRole.temperature) {
                temperatures.computeIfAbsent(point.time(), ignored -> new ArrayList<>()).add(point.value());
            } else if (role == PageWidgetPsychrometricSourceRole.humidity) {
                humidities.computeIfAbsent(point.time(), ignored -> new ArrayList<>()).add(point.value());
            }
        }
        List<ChartSeriesResponse> series = List.of(
                averagedSeries("temperature", "온도 평균", "°C", temperatures),
                averagedSeries("humidity", "상대습도 평균", "%", humidities)
        ).stream().filter(item -> !item.times().isEmpty()).toList();
        return response(widget, preset, window, range, "psychrometric", "per_device", List.of("°C", "%"), series);
    }

    private ChartWidgetResponse powerDistributionTrend(
            PageWidget widget, PageWidgetChartRangePreset preset, String window, QueryRanges.Range range
    ) {
        List<PageWidgetPowerDistributionGroup> groups = new ArrayList<>(widget.getPowerDistribution().getGroups());
        List<Integer> deviceIds = groups.stream().flatMap(group -> group.getSources().stream())
                .map(source -> source.getDevice().getId()).distinct().toList();
        List<String> pointNames = groups.stream().flatMap(group -> group.getSources().stream())
                .map(PageWidgetPowerDistributionSource::getPointName).distinct().toList();
        Map<String, String> groupBySource = new HashMap<>();
        for (PageWidgetPowerDistributionGroup group : groups) {
            for (PageWidgetPowerDistributionSource source : group.getSources()) {
                groupBySource.put(key(source.getDevice().getId(), source.getPointName()), group.getName());
            }
        }
        Map<String, Map<Instant, Double>> valuesByGroup = new LinkedHashMap<>();
        for (PageWidgetPowerDistributionGroup group : groups) valuesByGroup.put(group.getName(), new LinkedHashMap<>());
        for (SeriesPoint point : pointQuery.findSeries(deviceIds, pointNames, range.start(), range.end(), window)) {
            String group = groupBySource.get(key(point.deviceId(), point.pointName()));
            if (group != null) valuesByGroup.get(group).merge(point.time(), point.value(), Double::sum);
        }
        List<ChartSeriesResponse> series = new ArrayList<>();
        for (Map.Entry<String, Map<Instant, Double>> entry : valuesByGroup.entrySet()) {
            series.add(mapSeries(entry.getKey(), entry.getKey(), "W", entry.getValue()));
        }
        return response(widget, preset, window, range, "power_distribution", "sum", List.of("W"), series);
    }

    private List<Source> standardSources(PageWidget widget) {
        if (widget.getQueryKind() == PageWidgetQueryKind.last) {
            return widget.lastSourceDefinitions().stream()
                    .flatMap(source -> source.pointNames().stream().map(point -> new Source(source.device(), point)))
                    .toList();
        }
        List<String> pointNames = widget.pointNames();
        return widget.resolvedDefaultDevices().stream()
                .filter(Device::isEnabled)
                .flatMap(device -> pointNames.stream().map(point -> new Source(device, point)))
                .toList();
    }

    private List<ChartSeriesResponse> toSourceSeries(List<Source> sources, QueryRanges.Range range, String window) {
        if (sources.isEmpty()) return List.of();
        List<Integer> deviceIds = sources.stream().map(source -> source.device().getId()).distinct().toList();
        List<String> pointNames = sources.stream().map(Source::pointName).distinct().toList();
        Map<String, List<SeriesPoint>> valuesBySource = new LinkedHashMap<>();
        for (SeriesPoint point : pointQuery.findSeries(deviceIds, pointNames, range.start(), range.end(), window)) {
            valuesBySource.computeIfAbsent(key(point.deviceId(), point.pointName()), ignored -> new ArrayList<>()).add(point);
        }
        Map<String, String> unitBySource = unitsBySource(sources);
        List<ChartSeriesResponse> result = new ArrayList<>();
        for (Source source : sources) {
            String sourceKey = key(source.device().getId(), source.pointName());
            List<SeriesPoint> values = valuesBySource.getOrDefault(sourceKey, List.of());
            result.add(pointsSeries(sourceKey, source.device().getName() + " · " + source.pointName(),
                    source.device().getId(), source.pointName(), unitBySource.get(sourceKey), values));
        }
        return result;
    }

    private List<String> units(List<Source> sources) {
        return unitsBySource(sources).values().stream().filter(value -> value != null && !value.isBlank())
                .distinct().toList();
    }

    private Map<String, String> unitsBySource(List<Source> sources) {
        Set<Integer> modelIds = sources.stream().map(source -> source.device().getDeviceModel().getId())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> catalog = new HashMap<>();
        for (DeviceModelSnmpPoint point : deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(modelIds)) {
            catalog.put(key(point.getModelProtocol().getDeviceModel().getId(), point.getName()), point.getUnit());
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Source source : sources) {
            result.put(key(source.device().getId(), source.pointName()),
                    catalog.get(key(source.device().getDeviceModel().getId(), source.pointName())));
        }
        return result;
    }

    private static ChartSeriesResponse averagedSeries(String key, String label, String unit, Map<Instant, List<Double>> values) {
        Map<Instant, Double> averages = new LinkedHashMap<>();
        for (Map.Entry<Instant, List<Double>> entry : values.entrySet()) {
            averages.put(entry.getKey(), entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElseThrow());
        }
        return mapSeries(key, label, unit, averages);
    }

    private static ChartSeriesResponse mapSeries(String key, String label, String unit, Map<Instant, Double> values) {
        List<Map.Entry<Instant, Double>> sorted = values.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
        return new ChartSeriesResponse(key, label, null, null, null, unit, "left",
                sorted.stream().map(Map.Entry::getKey).toList(), sorted.stream().map(Map.Entry::getValue).toList());
    }

    private static ChartSeriesResponse pointsSeries(
            String key, String label, Integer deviceId, String pointName, String unit, List<SeriesPoint> points
    ) {
        List<SeriesPoint> sorted = points.stream().sorted(Comparator.comparing(SeriesPoint::time)).toList();
        return new ChartSeriesResponse(key, label, deviceId, pointName, null, unit, "left",
                sorted.stream().map(SeriesPoint::time).toList(), sorted.stream().map(SeriesPoint::value).toList());
    }

    private static ChartWidgetResponse response(
            PageWidget widget, PageWidgetChartRangePreset preset, String window, QueryRanges.Range range,
            String scope, String mode, List<String> units, List<ChartSeriesResponse> series
    ) {
        return new ChartWidgetResponse(widget.getId(), widget.getName(), widget.getPageCode().getCode(), scope, mode,
                preset.name(), window, range.start(), range.end(), units.size() == 1 ? units.get(0) : null,
                units, series, null);
    }

    private static void validateUnitCount(List<String> units) {
        if (units.size() > 2) {
            throw new IllegalArgumentException("trend supports at most two units, but found: " + String.join(", ", units));
        }
    }

    private static String resolveWindow(String window) {
        String value = window == null || window.isBlank() ? "15m" : window.trim();
        if (!Set.of("1m", "5m", "15m", "1h", "1d").contains(value)) {
            throw new IllegalArgumentException("window must be 1m, 5m, 15m, 1h, or 1d");
        }
        return value;
    }

    private static String key(Integer id, String pointName) {
        return id + "\u0000" + pointName.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private record Source(Device device, String pointName) {
    }
}
