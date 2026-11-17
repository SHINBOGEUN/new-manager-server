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
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.device.application.DeviceMeasurementSourceCatalog;
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
    private final DeviceRepository deviceRepository;
    private final PointQuery pointQuery;
    private final DeviceMeasurementSourceCatalog sourceCatalog;
    private final UnitNormalizer unitNormalizer;

    public ChartWidgetResponse getTrend(Integer widgetId, String rangePresetRaw, String window) {
        return getTrend(widgetId, rangePresetRaw, window, null);
    }

    public ChartWidgetResponse getTrend(Integer widgetId, String rangePresetRaw, String window, String compareMode) {
        return getTrend(widgetId, rangePresetRaw, window, "sum", compareMode);
    }

    public ChartWidgetResponse getTrend(Integer widgetId, String rangePresetRaw, String window,
                                        String seriesMode, String compareMode) {
        PageWidget widget = PageWidgetFinder.findRequired(pageWidgetRepository, widgetId);
        if (!widget.isEnabled()) throw new IllegalArgumentException("widget is disabled");
        PageWidgetChartRangePreset preset = PageWidgetChartRangePreset.from(rangePresetRaw);
        QueryRanges.Range range = QueryRanges.resolve(preset);
        ChartComparisonSupport.Comparison comparison = ChartComparisonSupport.resolve(compareMode, preset, range);
        String resolvedWindow = ChartComparisonSupport.windowForComparison(comparison, preset);
        if (comparison != null) range = comparison.currentRange();
        ChartWidgetResponse current = trendForRange(widget, preset, resolvedWindow, range);
        if (comparison == null) {
            if ("per_device".equalsIgnoreCase(seriesMode)) return current;
            if (current.units().size() > 1) {
                return current;
            }
            return withSeries(current, ChartComparisonSupport.sum(current.series(), current.unit(), "사용량 합계"), "sum");
        }
        if (current.units().size() > 1) throw new IllegalArgumentException("단위가 달라 합계를 낼 수 없습니다");
        ChartWidgetResponse previous = trendForRange(widget, preset, resolvedWindow, comparison.range());
        List<ChartSeriesResponse> currentValues = comparisonSeries(widget, current, range, resolvedWindow);
        List<ChartSeriesResponse> previousValues = comparisonSeries(widget, previous, comparison.range(), resolvedWindow);
        List<ChartSeriesResponse> currentSum = ChartComparisonSupport.sum(currentValues, current.unit(),
                "사용량 합계 (" + comparison.currentLabel() + ")");
        List<ChartSeriesResponse> previousSum = ChartComparisonSupport.sum(previousValues, previous.unit(),
                "사용량 합계 (" + comparison.previousLabel() + ")");
        List<ChartSeriesResponse> series = ChartComparisonSupport.appendComparison(currentSum, previousSum, comparison);
        return withSeries(current, series, "sum", List.of("sum"));
    }

    private static ChartWidgetResponse withSeries(ChartWidgetResponse current,
                                                   List<ChartSeriesResponse> series, String mode) {
        return withSeries(current, series, mode, current.availableSeriesModes());
    }

    private static ChartWidgetResponse withSeries(ChartWidgetResponse current,
                                                   List<ChartSeriesResponse> series, String mode,
                                                   List<String> availableModes) {
        return new ChartWidgetResponse(current.widgetId(), current.widgetName(), current.pageCode(),
                current.chartScope(), mode, current.rangePreset(), current.window(),
                current.start(), current.end(), current.unit(), current.units(), series,
                current.dataStatus(), availableModes);
    }

    private List<ChartSeriesResponse> comparisonSeries(PageWidget widget, ChartWidgetResponse response,
                                                        QueryRanges.Range range, String window) {
        if (widget.getQueryKind() != PageWidgetQueryKind.last && widget.getQueryKind() != PageWidgetQueryKind.aggregate) {
            return response.series();
        }
        return comparisonSeries(standardSources(widget), response.unit(), range, window);
    }

    private List<ChartSeriesResponse> comparisonSeries(List<Source> sources, String unit,
                                                        QueryRanges.Range range, String window) {
        List<Integer> deviceIds = sources.stream().map(source -> source.device().getId()).distinct().toList();
        List<String> pointNames = sources.stream().map(Source::pointName).distinct().toList();
        Map<String, String> typeBySource = new HashMap<>();
        for (DeviceMeasurementSourceCatalog.Source item : sourceCatalog.availableSources(
                sources.stream().map(source -> source.device().getId()).collect(java.util.stream.Collectors.toSet()))) {
            if (!item.ambiguous() && sources.stream().anyMatch(source -> source.device().getId().equals(item.deviceId())
                    && source.pointName().equalsIgnoreCase(item.pointName()))) {
                typeBySource.put(item.deviceId() + "\u0000" + item.pointName(), item.dataPointType());
            }
        }
        return List.of(ComparisonSeriesSupport.sum(pointQuery, deviceIds, pointNames, typeBySource,
                unit, range, window, ignored -> true, conversionPlan(sources)));
    }

    private ChartWidgetResponse trendForRange(PageWidget widget, PageWidgetChartRangePreset preset,
                                               String window, QueryRanges.Range range) {
        return switch (widget.getQueryKind()) {
            case last, aggregate -> standardTrend(widget, preset, window, range);
            case psychrometric -> psychrometricTrend(widget, preset, window, range);
            case power_distribution -> powerDistributionTrend(widget, preset, window, range);
            case chart -> throw new IllegalArgumentException("chart widget must use /query/chart");
            case calculated -> throw new IllegalArgumentException("calculated widget must use /query/calculated");
            case count -> throw new IllegalArgumentException("count widget has no time-series data");
            case cctv -> throw new IllegalArgumentException("CCTV widget has no time-series data");
        };
    }

    public ChartWidgetResponse previewTrend(List<String> sourceRefs, String rangePresetRaw, String window) {
        return previewTrend(sourceRefs, rangePresetRaw, window, "sum", "none");
    }

    public ChartWidgetResponse previewTrend(List<String> sourceRefs, String rangePresetRaw, String window,
                                            String seriesMode, String compareMode) {
        if (sourceRefs == null || sourceRefs.isEmpty()) {
            throw new IllegalArgumentException("먼저 데이터 선택에서 장비와 측정 항목을 선택하세요");
        }
        if (sourceRefs.size() > 50) {
            throw new IllegalArgumentException("미리보기는 최대 50개 장비·측정 항목까지 지원합니다");
        }

        List<Source> sources = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String sourceRef : sourceRefs) {
            if (sourceRef == null) throw new IllegalArgumentException("source 형식은 deviceId:pointName 이어야 합니다");
            int separator = sourceRef.indexOf(':');
            if (separator <= 0 || separator == sourceRef.length() - 1) {
                throw new IllegalArgumentException("source 형식은 deviceId:pointName 이어야 합니다");
            }
            int deviceId;
            try {
                deviceId = Integer.parseInt(sourceRef.substring(0, separator).trim());
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("source의 deviceId가 올바르지 않습니다");
            }
            if (deviceId <= 0) throw new IllegalArgumentException("source의 deviceId가 올바르지 않습니다");
            String pointName = sourceRef.substring(separator + 1).trim();
            if (pointName.isBlank() || pointName.length() > 255) {
                throw new IllegalArgumentException("source의 pointName이 비어 있거나 너무 깁니다");
            }
            String sourceKey = key(deviceId, pointName);
            if (!seen.add(sourceKey)) continue;
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new IllegalArgumentException("선택한 장비를 찾을 수 없습니다: " + deviceId));
            if (!device.isEnabled()) {
                throw new IllegalArgumentException("비활성 장비는 미리 볼 수 없습니다: " + device.getName());
            }
            sources.add(new Source(device, pointName));
        }

        PageWidgetChartRangePreset preset = PageWidgetChartRangePreset.from(rangePresetRaw);
        QueryRanges.Range range = QueryRanges.resolve(preset);
        ChartComparisonSupport.Comparison comparison = ChartComparisonSupport.resolve(compareMode, preset, range);
        String resolvedWindow = ChartComparisonSupport.windowForComparison(comparison, preset);
        if (comparison != null) range = comparison.currentRange();
        List<String> units = units(sources);
        String unit = units.size() == 1 ? units.get(0) : null;
        if (comparison != null && units.size() > 1) throw new IllegalArgumentException("단위가 달라 합계를 낼 수 없습니다");
        if (units.size() > 1 && comparison == null && !"per_device".equalsIgnoreCase(seriesMode)) {
            List<ChartSeriesResponse> series = toSourceSeries(sources, range, resolvedWindow);
            return new ChartWidgetResponse(null, "위젯 미리보기", null, "devices", "per_device",
                    preset.name(), resolvedWindow, range.start(), range.end(), null, units, series, null,
                    List.of("per_device"));
        }
        if ("per_device".equalsIgnoreCase(seriesMode) && comparison == null) {
            List<ChartSeriesResponse> series = toSourceSeries(sources, range, resolvedWindow);
            return new ChartWidgetResponse(null, "위젯 미리보기", null, "devices", "per_device",
                    preset.name(), resolvedWindow, range.start(), range.end(), unit, units, series, null,
                    List.of("sum", "per_device"));
        }
        List<ChartSeriesResponse> currentValues = comparisonSeries(sources, unit, range, resolvedWindow);
        List<ChartSeriesResponse> summed = ChartComparisonSupport.sum(currentValues, unit,
                comparison == null ? "사용량 합계" : "사용량 합계 (" + comparison.currentLabel() + ")");
        if (comparison != null) {
            List<ChartSeriesResponse> previous = ChartComparisonSupport.sum(
                    comparisonSeries(sources, unit, comparison.range(), resolvedWindow), unit,
                    "사용량 합계 (" + comparison.previousLabel() + ")");
            summed = ChartComparisonSupport.appendComparison(summed, previous, comparison);
        }
        return new ChartWidgetResponse(null, "위젯 미리보기", null, "devices", "sum",
                preset.name(), resolvedWindow, range.start(), range.end(), unit, units, summed, null,
                List.of("sum", "per_device"));
    }
    private ChartWidgetResponse standardTrend(
            PageWidget widget, PageWidgetChartRangePreset preset, String window, QueryRanges.Range range
    ) {
        List<Source> sources = standardSources(widget);
        List<ChartSeriesResponse> series = toSourceSeries(sources, range, window);
        List<String> units = units(sources);
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
        UnitNormalizer.Plan plan = conversionPlan(sources);
        Map<String, List<SeriesPoint>> valuesBySource = new LinkedHashMap<>();
        for (SeriesPoint raw : pointQuery.findSeries(deviceIds, pointNames, range.start(), range.end(), window)) {
            SeriesPoint point = plan.point(raw);
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

    private UnitNormalizer.Plan conversionPlan(List<Source> sources) {
        Set<Integer> ids = sources.stream().map(source -> source.device().getId()).collect(java.util.stream.Collectors.toSet());
        List<String> points = sources.stream().map(Source::pointName).distinct().toList();
        return unitNormalizer.plan(ids, points);
    }

    private Map<String, String> unitsBySource(List<Source> sources) {
        Set<Integer> deviceIds = sources.stream().map(source -> source.device().getId())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        UnitNormalizer.Plan plan = conversionPlan(sources);
        Map<String, String> catalog = new HashMap<>();
        for (DeviceMeasurementSourceCatalog.Source point : sourceCatalog.availableSources(deviceIds)) {
            if (deviceIds.contains(point.deviceId()) && !point.ambiguous()) {
                catalog.put(key(point.deviceId(), point.pointName()), plan.forSource(point.deviceId(), point.pointName()).unit());
            }
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Source source : sources) {
            result.put(key(source.device().getId(), source.pointName()),
                    catalog.get(key(source.device().getId(), source.pointName())));
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
                units, series, null, List.of("sum", "per_device"));
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
