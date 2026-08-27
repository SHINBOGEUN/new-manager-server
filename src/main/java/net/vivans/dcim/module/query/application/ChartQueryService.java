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
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.SeriesPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
        Range range = resolveRange(preset);

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
        String unit = resolveUnit(devices, pointNames);

        List<SeriesPoint> raw = pointQuery.findSeries(
                deviceIds, pointNames, range.start(), range.end(), window);

        List<ChartSeriesResponse> series = switch (mode) {
            case per_device -> buildPerDevice(raw, deviceById, pointNames);
            case sum -> buildSum(raw);
            case by_phase -> buildByPhase(raw);
            case by_path -> buildByPath(raw, deviceById);
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
                unit,
                series
        );
    }

    private List<ChartSeriesResponse> buildPerDevice(
            List<SeriesPoint> raw,
            Map<Integer, Device> deviceById,
            List<String> pointNames
    ) {
        Map<String, List<SeriesPoint>> grouped = new LinkedHashMap<>();
        for (SeriesPoint point : raw) {
            if (!deviceById.containsKey(point.deviceId())) {
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
                    points
            ));
        }
        series.sort(Comparator.comparing(ChartSeriesResponse::key));
        return series;
    }

    private List<ChartSeriesResponse> buildSum(List<SeriesPoint> raw) {
        Map<Instant, Double> sums = new HashMap<>();
        for (SeriesPoint point : raw) {
            sums.merge(point.time(), point.value(), Double::sum);
        }
        return List.of(toSeries("sum", "합계", null, null, null, fromInstantMap(sums)));
    }

    private List<ChartSeriesResponse> buildByPhase(List<SeriesPoint> raw) {
        Map<String, Map<Instant, Double>> byPoint = new LinkedHashMap<>();
        for (SeriesPoint point : raw) {
            byPoint.computeIfAbsent(point.pointName(), ignored -> new HashMap<>())
                    .merge(point.time(), point.value(), Double::sum);
        }
        List<ChartSeriesResponse> series = new ArrayList<>();
        for (Map.Entry<String, Map<Instant, Double>> entry : byPoint.entrySet()) {
            series.add(toSeries(
                    entry.getKey(), entry.getKey(), null, entry.getKey(), null,
                    fromInstantMap(entry.getValue())
            ));
        }
        series.sort(Comparator.comparing(ChartSeriesResponse::key));
        return series;
    }

    private List<ChartSeriesResponse> buildByPath(
            List<SeriesPoint> raw,
            Map<Integer, Device> deviceById
    ) {
        Map<String, Map<Instant, Double>> byPath = new LinkedHashMap<>();
        Map<String, String> labelByPath = new LinkedHashMap<>();
        for (SeriesPoint point : raw) {
            Device device = deviceById.get(point.deviceId());
            if (device == null || device.getLocationNode() == null) {
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
        return new ChartSeriesResponse(key, label, deviceId, pointName, locationNodeCode, times, values);
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
        List<Device> devices = new ArrayList<>();
        for (PageWidgetDevice mapping : widget.getDevices()) {
            Device device = mapping.getDevice();
            if (device.isEnabled()) {
                devices.add(device);
            }
        }
        return devices;
    }

    private String resolveUnit(List<Device> devices, List<String> pointNames) {
        Set<Integer> modelIds = devices.stream()
                .map(d -> d.getDeviceModel().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String unit = null;
        for (DeviceModelSnmpPoint point : deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(modelIds)) {
            if (!pointNames.contains(point.getName())) {
                continue;
            }
            String candidate = blankToNull(point.getUnit());
            if (candidate == null) {
                continue;
            }
            if (unit == null) {
                unit = candidate;
            } else if (!unit.equals(candidate)) {
                throw new IllegalArgumentException(
                        "chart points must share the same unit, but found " + unit + " and " + candidate);
            }
        }
        return unit;
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

    private static Range resolveRange(PageWidgetChartRangePreset preset) {
        Instant now = Instant.now();
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        return switch (preset) {
            case last_24h -> new Range(now.minus(24, ChronoUnit.HOURS), now);
            case today -> new Range(todayUtc.atStartOfDay().toInstant(ZoneOffset.UTC), now);
            case yesterday -> {
                LocalDate yesterday = todayUtc.minusDays(1);
                yield new Range(
                        yesterday.atStartOfDay().toInstant(ZoneOffset.UTC),
                        todayUtc.atStartOfDay().toInstant(ZoneOffset.UTC)
                );
            }
            case last_7d -> new Range(now.minus(7, ChronoUnit.DAYS), now);
            case this_month -> new Range(
                    todayUtc.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                    now
            );
        };
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
                List.of()
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

    private record Range(Instant start, Instant end) {
    }
}
