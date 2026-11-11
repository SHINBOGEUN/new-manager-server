package net.vivans.dcim.module.query.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartScope;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartSeriesMode;
import net.vivans.dcim.module.device.domain.model.PageWidgetDevice;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.device.application.DeviceMeasurementSourceCatalog;
import net.vivans.dcim.module.query.api.dto.ChartSeriesResponse;
import net.vivans.dcim.module.query.api.dto.ChartWidgetResponse;
import net.vivans.dcim.module.query.api.dto.WidgetDataStatusResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.SeriesPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChartQueryService {

    static final int MAX_DEVICE_IDS = 200;
    private static final Set<String> PHASE_CODES = Set.of("P1", "P2", "P3");

    private static boolean isPhaseCode(String phase) {
        return phase != null && PHASE_CODES.contains(phase);
    }

    private static boolean containsType(Set<String> types, String type) {
        return type != null && types.contains(type);
    }

    private final PageWidgetRepository pageWidgetRepository;
    private final DeviceRepository deviceRepository;
    private final PointQuery pointQuery;
    private final DeviceMeasurementSourceCatalog sourceCatalog;
    private final WidgetDataStatusResolver widgetDataStatusResolver;

    public ChartWidgetResponse getChart(
            Integer widgetId,
            String rangePresetOverride,
            String windowOverride,
            String seriesModeOverride
    ) {
        PageWidget widget = findChartWidget(widgetId);
        List<String> pointNames = widget.pointNames();
        PageWidgetChartRangePreset preset = resolvePreset(widget, rangePresetOverride);
        String window = resolveWindow(widget, windowOverride);
        PageWidgetChartSeriesMode mode = resolveSeriesMode(widget, seriesModeOverride);
        QueryRanges.Range range = QueryRanges.resolve(preset);

        if (pointNames.isEmpty()) {
            return empty(widget, range.start(), range.end(), window, null, mode, preset);
        }

        List<Device> devices = resolveDevices(widget);
        if (devices.isEmpty()) {
            return empty(widget, range.start(), range.end(), window, null, mode, preset);
        }
        if (devices.size() > MAX_DEVICE_IDS) {
            throw new IllegalArgumentException(
                    "chart resolves to more than " + MAX_DEVICE_IDS + " enabled devices");
        }

        List<Integer> deviceIds = devices.stream().map(Device::getId).toList();
        Map<Integer, Device> deviceById = devices.stream()
                .collect(Collectors.toMap(Device::getId, d -> d, (a, b) -> a, LinkedHashMap::new));
        List<String> queryPointNames = pointNames;
        ChartPointContext pointContext = buildPointContext(devices, pointNames);
        ChartUnitContext unitContext = buildUnitContext(devices, queryPointNames);
        if (unitContext.units().size() > 1 && mode != PageWidgetChartSeriesMode.per_device) {
            throw new IllegalArgumentException(
                    "two-unit charts support only per_device series mode; select per_device to avoid summing different units");
        }

        List<SeriesPoint> raw = pointQuery.findSeries(
                deviceIds, queryPointNames, range.start(), range.end(), window);
        Map<String, LastPoint> latestBySource = new HashMap<>();
        for (LastPoint point : pointQuery.findLast(deviceIds, queryPointNames, Duration.ofHours(24))) {
            latestBySource.put(sourceKey(point.deviceId(), point.pointName()), point);
        }
        List<Instant> collectedTimes = new ArrayList<>();
        for (Integer deviceId : deviceIds) {
            for (String pointName : queryPointNames) {
                LastPoint point = latestBySource.get(sourceKey(deviceId, pointName));
                collectedTimes.add(point == null ? null : point.time());
            }
        }
        WidgetDataStatusResponse dataStatus = widgetDataStatusResolver
                .resolve(collectedTimes, widget.getDataFreshnessMinutes());

        List<ChartSeriesResponse> series = switch (mode) {
            case per_device -> buildPerDevice(raw, deviceById, queryPointNames, pointContext, unitContext);
            case sum -> buildSum(raw, deviceById, pointContext, unitContext);
            case by_phase -> buildByPhase(raw, deviceById, pointContext, unitContext);
            case by_path -> buildByPath(raw, deviceById, pointContext, unitContext);
        };

        PageWidgetChartScope scope = widget.getChartScope() == null
                ? PageWidgetChartScope.devices
                : widget.getChartScope();

        return new ChartWidgetResponse(
                widget.getId(),
                widget.getName(),
                widget.getPageCode().getCode(),
                scope.name(),
                mode.name(),
                preset.name(),
                window,
                range.start(),
                range.end(),
                unitContext.singleUnit(),
                unitContext.units(),
                series,
                dataStatus,
                availableSeriesModes(devices, pointContext, unitContext)
        );
    }

    private ChartPointContext buildPointContext(List<Device> devices, List<String> pointNames) {
        Set<Integer> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toSet());
        Map<String, DeviceMeasurementSourceCatalog.Source> sources = new HashMap<>();
        Map<Integer, Map<String, Set<String>>> phasesByDeviceAndType = new HashMap<>();
        Map<Integer, Set<String>> unphasedTypesByDevice = new HashMap<>();
        for (DeviceMeasurementSourceCatalog.Source source : sourceCatalog.availableSources(deviceIds)) {
            if (source.ambiguous() || !pointNames.contains(source.pointName())) continue;
            sources.putIfAbsent(sourceKey(source.deviceId(), source.pointName()), source);
            String type = source.dataPointType();
            if (type == null || type.isBlank()) continue;
            if (source.phase() == null || source.phase().isBlank()) {
                unphasedTypesByDevice.computeIfAbsent(source.deviceId(), ignored -> new HashSet<>()).add(type);
            } else if (isPhaseCode(source.phase())) {
                phasesByDeviceAndType.computeIfAbsent(source.deviceId(), ignored -> new HashMap<>())
                        .computeIfAbsent(type, ignored -> new HashSet<>()).add(source.phase());
            }
        }
        Map<Integer, Set<String>> phaseTypesByDevice = new HashMap<>();
        Set<String> phaseTypes = new LinkedHashSet<>();
        phasesByDeviceAndType.forEach((deviceId, byType) -> byType.forEach((type, phases) -> {
            if (phases.containsAll(Set.of("P1", "P2", "P3"))) {
                phaseTypesByDevice.computeIfAbsent(deviceId, ignored -> new HashSet<>()).add(type);
                phaseTypes.add(type);
            }
        }));
        return new ChartPointContext(sources, phaseTypesByDevice, unphasedTypesByDevice, phaseTypes, pointNames);
    }

    private static boolean includePerDevicePoint(SeriesPoint point, ChartPointContext context) {
        var source = context.sourcesByKey().get(sourceKey(point.deviceId(), point.pointName()));
        if (source == null || source.dataPointType() == null || source.phase() == null) return true;
        return !isPhaseCode(source.phase())
                || !containsType(context.phaseTypesByDevice().getOrDefault(point.deviceId(), Set.of()), source.dataPointType())
                || !containsType(context.unphasedTypesByDevice().getOrDefault(point.deviceId(), Set.of()), source.dataPointType());
    }

    private static boolean includeAggregatePoint(
            SeriesPoint point,
            Map<Integer, Device> deviceById,
            ChartPointContext context
    ) {
        var source = context.sourcesByKey().get(sourceKey(point.deviceId(), point.pointName()));
        if (source == null) return true;
        if ("N".equals(source.phase())) return false;
        String type = source.dataPointType();
        if (type == null || !context.phaseTypes().contains(type)) return true;
        boolean phaseDevice = containsType(context.phaseTypesByDevice().getOrDefault(point.deviceId(), Set.of()), type);
        if (phaseDevice) return isPhaseCode(source.phase());
        return source.phase() == null || source.phase().isBlank();
    }

    private record ChartPointContext(
            Map<String, DeviceMeasurementSourceCatalog.Source> sourcesByKey,
            Map<Integer, Set<String>> phaseTypesByDevice,
            Map<Integer, Set<String>> unphasedTypesByDevice,
            Set<String> phaseTypes,
            List<String> pointNames
    ) {
    }
    private List<ChartSeriesResponse> buildPerDevice(
            List<SeriesPoint> raw,
            Map<Integer, Device> deviceById,
            List<String> pointNames,
            ChartPointContext context,
            ChartUnitContext unitContext
    ) {
        Map<String, List<SeriesPoint>> grouped = new LinkedHashMap<>();
        for (SeriesPoint point : raw) {
            if (!deviceById.containsKey(point.deviceId())) {
                continue;
            }
            if (!includePerDevicePoint(point, context)) {
                continue;
            }
            String key = point.deviceId() + ":" + point.pointName();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(point);
        }
        List<ChartSeriesResponse> series = new ArrayList<>();
        for (Map.Entry<String, List<SeriesPoint>> entry : grouped.entrySet()) {
            List<SeriesPoint> points = entry.getValue();
            points.sort(Comparator.comparing(SeriesPoint::time));
            SeriesPoint first = points.get(0);
            Device device = deviceById.get(first.deviceId());
            String label = pointNames.size() == 1
                    ? device.getName()
                    : device.getName() + " · " + first.pointName();
            series.add(toSeries(
                    entry.getKey(),
                    label,
                    device.getId(),
                    first.pointName(),
                    device.getLocationNode() == null ? null : device.getLocationNode().getCode(),
                    unitContext.unitFor(first.deviceId(), first.pointName()),
                    unitContext.axisFor(first.deviceId(), first.pointName()),
                    points
            ));
        }
        series.sort(Comparator.comparing(ChartSeriesResponse::key));
        return series;
    }

    private List<ChartSeriesResponse> buildSum(
            List<SeriesPoint> raw,
            Map<Integer, Device> deviceById,
            ChartPointContext context,
            ChartUnitContext unitContext
    ) {
        Map<Instant, Double> sums = new HashMap<>();
        for (SeriesPoint point : raw) {
            if (!includeAggregatePoint(point, deviceById, context)) {
                continue;
            }
            sums.merge(point.time(), point.value(), Double::sum);
        }
        return List.of(toSeries("sum", "합계", null, null, null, unitContext.singleUnit(), "left", fromInstantMap(sums)));
    }

    private List<ChartSeriesResponse> buildByPhase(
            List<SeriesPoint> raw,
            Map<Integer, Device> deviceById,
            ChartPointContext context,
            ChartUnitContext unitContext
    ) {
        if (context.phaseTypes().isEmpty()) {
            return buildPerDevice(raw, deviceById, context.pointNames(), context, unitContext);
        }
        Map<String, Map<Instant, Double>> grouped = new LinkedHashMap<>();
        Map<String, String> labels = new HashMap<>();
        Map<String, java.util.SortedMap<PhaseNotationKey, String>> phaseLabels = new HashMap<>();
        boolean multipleTypes = context.phaseTypes().size() > 1;
        for (SeriesPoint point : raw) {
            var source = context.sourcesByKey().get(sourceKey(point.deviceId(), point.pointName()));
            if (source == null || source.dataPointType() == null) continue;
            String type = source.dataPointType();
            String phase = source.phase();
            String key;
            String label;
            if (isPhaseCode(phase)
                    && containsType(context.phaseTypesByDevice().getOrDefault(point.deviceId(), Set.of()), type)) {
                key = multipleTypes ? type + ":" + phase : phase;
                String display = source.phaseLabel() == null ? phase : source.phaseLabel();
                int notationOrder = source.phaseNotationOrder() == null ? 1 : source.phaseNotationOrder();
                String notationCode = source.phaseNotationCode() == null ? display : source.phaseNotationCode();
                phaseLabels.computeIfAbsent(key, ignored -> new java.util.TreeMap<>())
                        .putIfAbsent(new PhaseNotationKey(notationOrder, notationCode), display);
                label = null;
            } else if ((phase == null || phase.isBlank()) && context.phaseTypes().contains(type)
                    && !containsType(context.phaseTypesByDevice().getOrDefault(point.deviceId(), Set.of()), type)) {
                key = "single:" + type;
                label = multipleTypes ? "단상 (" + type + ")" : "단상";
            } else {
                continue;
            }
            grouped.computeIfAbsent(key, ignored -> new HashMap<>()).merge(point.time(), point.value(), Double::sum);
            if (label != null) labels.putIfAbsent(key, label);
        }
        phaseLabels.forEach((key, notation) -> {
            String label = String.join("/", new LinkedHashSet<>(notation.values())) + "상";
            String type = key.contains(":") ? key.substring(0, key.indexOf(':')) : null;
            labels.put(key, type == null ? label : label + " (" + type + ")");
        });
        List<ChartSeriesResponse> series = new ArrayList<>();
        for (Map.Entry<String, Map<Instant, Double>> entry : grouped.entrySet()) {
            series.add(toSeries(entry.getKey(), labels.getOrDefault(entry.getKey(), entry.getKey()), null,
                    entry.getKey(), null, unitContext.singleUnit(), "left", fromInstantMap(entry.getValue())));
        }
        series.sort(Comparator.comparing(ChartSeriesResponse::key));
        return series;
    }
    private List<ChartSeriesResponse> buildByPath(
            List<SeriesPoint> raw,
            Map<Integer, Device> deviceById,
            ChartPointContext context,
            ChartUnitContext unitContext
    ) {
        Map<String, Map<Instant, Double>> byPath = new LinkedHashMap<>();
        Map<String, String> labelByPath = new LinkedHashMap<>();
        for (SeriesPoint point : raw) {
            Device device = deviceById.get(point.deviceId());
            if (device == null || device.getLocationNode() == null) {
                continue;
            }
            if (!includeAggregatePoint(point, deviceById, context)) {
                continue;
            }
            // 장비별 pathCode (A/B/C). 위치가 달라도 같은 path code면 합산.
            var path = device.getPathCode();
            String key;
            String label;
            if (path != null) {
                key = path.getCode();
                label = path.getName() != null ? path.getName() : path.getCode();
            } else {
                key = "_none";
                label = "Path 미지정";
            }
            labelByPath.putIfAbsent(key, label);
            byPath.computeIfAbsent(key, ignored -> new HashMap<>())
                    .merge(point.time(), point.value(), Double::sum);
        }
        List<ChartSeriesResponse> series = new ArrayList<>();
        for (Map.Entry<String, Map<Instant, Double>> entry : byPath.entrySet()) {
            String key = entry.getKey();
            series.add(toSeries(
                    key,
                    labelByPath.getOrDefault(key, key),
                    null,
                    null,
                    "_none".equals(key) ? null : key,
                    unitContext.singleUnit(),
                    "left",
                    fromInstantMap(entry.getValue())
            ));
        }
        series.sort(Comparator.comparing(ChartSeriesResponse::key));
        return series;
    }

    private static ChartSeriesResponse toSeries(
            String key,
            String label,
            Integer deviceId,
            String pointName,
            String locationNodeCode,
            String unit,
            String axis,
            List<SeriesPoint> points
    ) {
        List<SeriesPoint> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.comparing(SeriesPoint::time));
        List<Instant> times = new ArrayList<>(sorted.size());
        List<Double> values = new ArrayList<>(sorted.size());
        for (SeriesPoint point : sorted) {
            times.add(point.time());
            values.add(point.value());
        }
        return new ChartSeriesResponse(key, label, deviceId, pointName, locationNodeCode, unit, axis, times, values);
    }

    private static List<SeriesPoint> fromInstantMap(Map<Instant, Double> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new SeriesPoint(0, "", e.getValue(), e.getKey()))
                .toList();
    }

    private List<Device> resolveDevices(PageWidget widget) {
        PageWidgetChartScope scope = widget.getChartScope() == null
                ? PageWidgetChartScope.devices
                : widget.getChartScope();
        if (scope == PageWidgetChartScope.models) {
            List<Integer> modelIds = widget.modelIds();
            if (modelIds.isEmpty()) {
                return List.of();
            }
            return deviceRepository.findAllEnabledByDeviceModelIds(modelIds);
        }
        List<Device> resolved = widget.resolvedDefaultDevices().stream()
                .filter(Device::isEnabled)
                .toList();
        if (!resolved.isEmpty()) {
            return resolved;
        }
        // Keep legacy direct-device widgets usable when no group target resolves.
        return widget.getDevices().stream()
                .map(mapping -> mapping.getDevice())
                .filter(Device::isEnabled)
                .toList();
    }

    private static List<String> availableSeriesModes(
            List<Device> devices, ChartPointContext context, ChartUnitContext unitContext
    ) {
        if (unitContext.units().size() > 1) return List.of("per_device");
        List<String> modes = new ArrayList<>(List.of("sum", "per_device"));
        if (!context.phaseTypes().isEmpty()) modes.add("by_phase");
        long locatedDevices = devices.stream().filter(device -> device.getLocationNode() != null).count();
        if (locatedDevices >= 2) modes.add("by_path");
        return List.copyOf(modes);
    }
    private ChartUnitContext buildUnitContext(List<Device> devices, List<String> pointNames) {
        Set<Integer> deviceIds = devices.stream()
                .map(Device::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> units = new LinkedHashSet<>();
        Map<String, String> unitsBySource = new HashMap<>();
        for (DeviceMeasurementSourceCatalog.Source point : sourceCatalog.availableSources(deviceIds)) {
            if (!deviceIds.contains(point.deviceId()) || point.ambiguous()
                    || !pointNames.contains(point.pointName())) {
                continue;
            }
            String candidate = blankToNull(point.unit());
            if (candidate != null) {
                unitsBySource.put(sourceKey(point.deviceId(), point.pointName()), candidate);
                units.add(candidate);
            }
        }
        return new ChartUnitContext(unitsBySource, List.copyOf(units));
    }

    private static String sourceKey(int deviceId, String pointName) {
        return deviceId + "\u0000" + pointName;
    }

    private record PhaseNotationKey(int sortOrder, String code) implements Comparable<PhaseNotationKey> {
        @Override
        public int compareTo(PhaseNotationKey other) {
            int order = Integer.compare(sortOrder, other.sortOrder);
            return order != 0 ? order : code.compareTo(other.code);
        }
    }

    private record ChartUnitContext(Map<String, String> unitsBySource, List<String> units) {

        String unitFor(int deviceId, String pointName) {
            return unitsBySource.get(sourceKey(deviceId, pointName));
        }

        String axisFor(int deviceId, String pointName) {
            String unit = unitFor(deviceId, pointName);
            return units.size() == 2 && unit != null && unit.equals(units.get(1)) ? "right" : "left";
        }

        String singleUnit() {
            return units.size() == 1 ? units.get(0) : null;
        }
    }

    private static PageWidgetChartRangePreset resolvePreset(PageWidget widget, String override) {
        if (override != null && !override.isBlank()) {
            return PageWidgetChartRangePreset.from(override);
        }
        return widget.getChartRangePreset() == null
                ? PageWidgetChartRangePreset.last_24h
                : widget.getChartRangePreset();
    }

    private static PageWidgetChartSeriesMode resolveSeriesMode(PageWidget widget, String override) {
        if (override != null && !override.isBlank()) {
            return PageWidgetChartSeriesMode.from(override);
        }
        return widget.getChartSeriesMode() == null
                ? PageWidgetChartSeriesMode.per_device
                : widget.getChartSeriesMode();
    }

    private static String resolveWindow(PageWidget widget, String override) {
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        return widget.getChartWindow() == null ? "5m" : widget.getChartWindow();
    }

    private ChartWidgetResponse empty(
            PageWidget widget,
            Instant start,
            Instant end,
            String window,
            String unit,
            PageWidgetChartSeriesMode mode,
            PageWidgetChartRangePreset preset
    ) {
        PageWidgetChartScope scope = widget.getChartScope() == null
                ? PageWidgetChartScope.devices
                : widget.getChartScope();
        return new ChartWidgetResponse(
                widget.getId(),
                widget.getName(),
                widget.getPageCode().getCode(),
                scope.name(),
                mode.name(),
                preset.name(),
                window,
                start,
                end,
                unit,
                unit == null ? List.of() : List.of(unit),
                List.of(),
                widgetDataStatusResolver.resolve(List.of(), widget.getDataFreshnessMinutes()),
                List.of("sum", "per_device")
        );
    }

    private PageWidget findChartWidget(Integer widgetId) {
        PageWidget widget = PageWidgetFinder.findRequired(pageWidgetRepository, widgetId);
        if (widget.getQueryKind() != PageWidgetQueryKind.chart) {
            throw new IllegalArgumentException(
                    "widget queryKind must be chart, but was " + widget.getQueryKind());
        }
        if (!widget.isEnabled()) {
            throw new IllegalArgumentException("widget is disabled");
        }
        return widget;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
