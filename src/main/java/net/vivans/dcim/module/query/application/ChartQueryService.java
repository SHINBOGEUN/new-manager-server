package net.vivans.dcim.module.query.application;

import jakarta.persistence.EntityNotFoundException;
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
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
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

    private final PageWidgetRepository pageWidgetRepository;
    private final DeviceRepository deviceRepository;
    private final PointQuery pointQuery;
    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;
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
        if (unitContext.units().size() > 2) {
            throw new IllegalArgumentException("chart supports at most two units, but found: "
                    + String.join(", ", unitContext.units()));
        }
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
                dataStatus
        );
    }

    private static String phaseSuffix(String name) {
        if (name == null || name.length() < 4 || name.charAt(0) != 'L'
                || (name.charAt(1) != '1' && name.charAt(1) != '2' && name.charAt(1) != '3')
                || name.charAt(2) != '_') {
            return null;
        }
        return name.substring(3);
    }

    private static String totalSuffix(String name) {
        if (name == null) return null;
        return switch (name) {
            case "TOTAL_WT" -> "WATT";
            case "AMP" -> "AMP";
            case "PF" -> "PF";
            default -> name.startsWith("TOTAL_") ? name.substring(6) : null;
        };
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
            if (!includePerDevicePoint(point.pointName(), context)) {
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
        Map<String, Map<Instant, Double>> byPoint = new LinkedHashMap<>();
        Map<String, String> labels = new HashMap<>();
        for (SeriesPoint point : raw) {
            if (context.phaseSuffixes().isEmpty()) {
                byPoint.computeIfAbsent(point.pointName(), ignored -> new HashMap<>())
                        .merge(point.time(), point.value(), Double::sum);
                labels.putIfAbsent(point.pointName(), point.pointName());
                continue;
            }
            Device device = deviceById.get(point.deviceId());
            if (device == null) continue;
            boolean phaseModel = context.phaseModelIds().contains(device.getDeviceModel().getId());
            String suffix = phaseSuffix(point.pointName());
            String total = totalSuffix(point.pointName());
            String key;
            if (phaseModel && suffix != null && context.phaseSuffixes().contains(suffix)) {
                key = point.pointName();
                labels.putIfAbsent(key, key);
            } else if (!phaseModel && total != null && context.phaseSuffixes().contains(total)) {
                key = "single:" + total;
                labels.putIfAbsent(key, context.phaseSuffixes().size() == 1 ? "단상" : "단상 " + total);
            } else {
                continue;
            }
            byPoint.computeIfAbsent(key, ignored -> new HashMap<>())
                    .merge(point.time(), point.value(), Double::sum);
        }
        List<ChartSeriesResponse> series = new ArrayList<>();
        for (Map.Entry<String, Map<Instant, Double>> entry : byPoint.entrySet()) {
            series.add(toSeries(
                    entry.getKey(), labels.getOrDefault(entry.getKey(), entry.getKey()), null, entry.getKey(), null,
                    unitContext.singleUnit(), "left",
                    fromInstantMap(entry.getValue())
            ));
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
            // PDU(장비)별 pathCode (A/B/C). 위치가 달라도 같은 path code면 합산.
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

    private ChartPointContext buildPointContext(List<Device> devices, List<String> pointNames) {
        Set<String> phaseSuffixes = pointNames.stream()
                .map(ChartQueryService::phaseSuffix)
                .filter(java.util.Objects::nonNull)
                .filter(suffix -> pointNames.contains("L1_" + suffix)
                        && pointNames.contains("L2_" + suffix)
                        && pointNames.contains("L3_" + suffix))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> totalSuffixes = pointNames.stream()
                .map(ChartQueryService::totalSuffix)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (phaseSuffixes.isEmpty()) {
            return new ChartPointContext(Set.of(), Set.of(), totalSuffixes);
        }

        Set<Integer> modelIds = devices.stream()
                .map(device -> device.getDeviceModel().getId())
                .collect(Collectors.toSet());
        Map<Integer, Set<String>> namesByModel = new HashMap<>();
        for (DeviceModelSnmpPoint point : deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(modelIds)) {
            Integer modelId = point.getModelProtocol().getDeviceModel().getId();
            namesByModel.computeIfAbsent(modelId, ignored -> new HashSet<>()).add(point.getName());
        }
        Set<Integer> phaseModelIds = new HashSet<>();
        for (Map.Entry<Integer, Set<String>> entry : namesByModel.entrySet()) {
            for (String suffix : phaseSuffixes) {
                Set<String> names = entry.getValue();
                if (names.contains("L1_" + suffix) && names.contains("L2_" + suffix)
                        && names.contains("L3_" + suffix)) {
                    phaseModelIds.add(entry.getKey());
                    break;
                }
            }
        }
        return new ChartPointContext(phaseSuffixes, phaseModelIds, totalSuffixes);
    }

    private static boolean includePerDevicePoint(String pointName, ChartPointContext context) {
        String phase = phaseSuffix(pointName);
        String total = totalSuffix(pointName);
        if (phase != null && context.phaseSuffixes().contains(phase)
                && context.totalSuffixes().contains(phase)) {
            return false;
        }
        return total == null || !context.phaseSuffixes().contains(total)
                || context.totalSuffixes().contains(total);
    }

    private static boolean includeAggregatePoint(
            SeriesPoint point,
            Map<Integer, Device> deviceById,
            ChartPointContext context
    ) {
        String phase = phaseSuffix(point.pointName());
        String total = totalSuffix(point.pointName());
        if ((phase == null || !context.phaseSuffixes().contains(phase))
                && (total == null || !context.phaseSuffixes().contains(total))) {
            return true;
        }
        Device device = deviceById.get(point.deviceId());
        if (device == null) return false;
        boolean phaseModel = context.phaseModelIds().contains(device.getDeviceModel().getId());
        return phaseModel
                ? phase != null && context.phaseSuffixes().contains(phase)
                : total != null && context.phaseSuffixes().contains(total);
    }

    private record ChartPointContext(
            Set<String> phaseSuffixes,
            Set<Integer> phaseModelIds,
            Set<String> totalSuffixes
    ) {
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

    private ChartUnitContext buildUnitContext(List<Device> devices, List<String> pointNames) {
        Set<Integer> modelIds = devices.stream()
                .map(d -> d.getDeviceModel().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Integer, Map<String, String>> unitsByModel = new HashMap<>();
        LinkedHashSet<String> units = new LinkedHashSet<>();
        for (DeviceModelSnmpPoint point : deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(modelIds)) {
            if (!pointNames.contains(point.getName())) {
                continue;
            }
            String candidate = blankToNull(point.getUnit());
            if (candidate != null) {
                Integer modelId = point.getModelProtocol().getDeviceModel().getId();
                unitsByModel.computeIfAbsent(modelId, ignored -> new HashMap<>()).put(point.getName(), candidate);
            }
        }
        Map<String, String> unitsBySource = new HashMap<>();
        for (Device device : devices) {
            Map<String, String> byPoint = unitsByModel.getOrDefault(device.getDeviceModel().getId(), Map.of());
            for (String pointName : pointNames) {
                String unit = byPoint.get(pointName);
                if (unit != null) {
                    unitsBySource.put(sourceKey(device.getId(), pointName), unit);
                    units.add(unit);
                }
            }
        }
        return new ChartUnitContext(unitsBySource, List.copyOf(units));
    }

    private static String sourceKey(int deviceId, String pointName) {
        return deviceId + "\u0000" + pointName;
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
                widgetDataStatusResolver.resolve(List.of(), widget.getDataFreshnessMinutes())
        );
    }

    private PageWidget findChartWidget(Integer widgetId) {
        if (widgetId == null) {
            throw new IllegalArgumentException("widgetId is required");
        }
        PageWidget widget = pageWidgetRepository.findById(widgetId)
                .orElseThrow(() -> new EntityNotFoundException("PageWidget not found: " + widgetId));
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
