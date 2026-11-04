package net.vivans.dcim.module.query.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.device.domain.model.PageWidgetDevice;
import net.vivans.dcim.module.device.domain.model.PageWidgetDeviceRole;
import net.vivans.dcim.module.device.domain.model.PageWidgetOp;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.device.application.DeviceMeasurementSourceCatalog;
import net.vivans.dcim.module.query.api.dto.AggregateDeviceValueResponse;
import net.vivans.dcim.module.query.api.dto.AggregateWidgetResponse;
import net.vivans.dcim.module.query.api.dto.WidgetDataStatusResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AggregateQueryService {

    static final int MAX_DEVICE_IDS = 200;

    private final PageWidgetRepository pageWidgetRepository;
    private final PointQuery pointQuery;
    private final DeviceMeasurementSourceCatalog sourceCatalog;
    private final WidgetDataStatusResolver widgetDataStatusResolver;

    public AggregateWidgetResponse getAggregate(Integer widgetId, String rangePresetOverride) {
        PageWidget widget = findAggregateWidget(widgetId);
        PageWidgetOp preset = widget.getOp();
        if (preset == null) {
            throw new IllegalArgumentException("aggregatePreset/op is required");
        }
        PageWidgetChartRangePreset rangePreset = resolvePreset(widget, preset, rangePresetOverride);
        QueryRanges.Range range = switch (preset) {
            case usage_rank, power_rank, demand -> QueryRanges.resolve(rangePreset, java.time.ZoneId.of("Asia/Seoul"));
            default -> QueryRanges.resolve(rangePreset);
        };

        AggregateWidgetResponse response = switch (preset) {
            case usage, usage_rank -> computeUsage(widget, preset, rangePreset, range);
            case power, power_rank -> computePower(widget, preset, rangePreset, range);
            case demand -> computeDemand(widget, preset, rangePreset, range);
        };
        return withDataStatus(widget, response);
    }

    private AggregateWidgetResponse computeDemand(PageWidget widget, PageWidgetOp preset,
            PageWidgetChartRangePreset rangePreset, QueryRanges.Range range) {
        List<Device> devices = resolveEnabledDevices(widget, PageWidgetDeviceRole.DEFAULT, PageWidgetDeviceRole.TOTAL);
        String pointName = requireAggregatePoint(widget);
        String unit = resolveUnit(devices, List.of(pointName));
        if (devices.isEmpty()) return empty(widget, preset, rangePreset, range, unit);
        validateRankingSources(devices, pointName, "POWER");
        if (!"W".equalsIgnoreCase(unit) && !"kW".equalsIgnoreCase(unit)) {
            throw new IllegalArgumentException("최대수요전력은 동일한 W 또는 kW 단위의 전력 항목이 필요합니다");
        }
        List<Integer> ids = devices.stream().map(Device::getId).toList();
        var points = pointQuery.findSeries(ids, List.of(pointName), range.start(), range.end(), "15m");
        var peak = PowerDemandCalculator.peak(points, ids, range.start(), range.end());
        if (peak == null) return empty(widget, preset, rangePreset, range, unit);
        return new AggregateWidgetResponse(widget.getId(), widget.getName(), widget.getPageCode().getCode(),
                preset.name(), rangePreset.name(), range.start(), range.end(), QueryValues.round2(peak.value()),
                unit, ids.size(), List.of(), null, peak.time());
    }

    private AggregateWidgetResponse computeUsage(
            PageWidget widget,
            PageWidgetOp preset,
            PageWidgetChartRangePreset rangePreset,
            QueryRanges.Range range
    ) {
        List<Device> devices = resolveEnabledDevices(widget, PageWidgetDeviceRole.DEFAULT, PageWidgetDeviceRole.TOTAL);
        if (devices.isEmpty()) {
            return empty(widget, preset, rangePreset, range, null);
        }
        List<Integer> deviceIds = devices.stream().map(Device::getId).toList();
        Map<Integer, Device> deviceById = indexDevices(devices);
        String pointName = requireAggregatePoint(widget);
        if (preset == PageWidgetOp.usage_rank) validateRankingSources(devices, pointName, "ENERGY");
        List<String> pointNames = List.of(pointName);
        String unit = resolveUnit(devices, pointNames);

        List<LastPoint> firsts = pointQuery.findFirstInRange(
                deviceIds, pointNames, range.start(), range.end());
        List<LastPoint> lasts = pointQuery.findLastInRange(
                deviceIds, pointNames, range.start(), range.end());

        Map<String, Double> firstByKey = indexByDevicePoint(firsts);
        Map<String, Double> lastByKey = indexByDevicePoint(lasts);

        List<AggregateDeviceValueResponse> deviceRows = new ArrayList<>();
        double sum = 0;
        for (Integer deviceId : deviceIds) {
            String key = key(deviceId, pointName);
            Double first = firstByKey.get(key);
            Double last = lastByKey.get(key);
            if (first == null || last == null) {
                continue;
            }
            double delta = last - first;
            if (delta < 0) {
                continue;
            }
            Device device = deviceById.get(deviceId);
            deviceRows.add(new AggregateDeviceValueResponse(
                    deviceId,
                    device == null ? null : device.getName(),
                    PageWidgetDeviceRole.DEFAULT.wireValue(),
                    QueryValues.round2(delta)
            ));
            sum += delta;
        }

        Double value = deviceRows.isEmpty() ? null : sum;
        return response(widget, preset, rangePreset, range, value, unit, deviceRows);
    }

    private AggregateWidgetResponse computePower(
            PageWidget widget,
            PageWidgetOp preset,
            PageWidgetChartRangePreset rangePreset,
            QueryRanges.Range range
    ) {
        List<Device> devices = resolveEnabledDevices(widget, PageWidgetDeviceRole.DEFAULT, PageWidgetDeviceRole.TOTAL);
        if (devices.isEmpty()) {
            return empty(widget, preset, rangePreset, range, null);
        }
        if (preset == PageWidgetOp.power_rank) validateRankingSources(devices, requireAggregatePoint(widget), "POWER");
        return sumLastPower(widget, preset, rangePreset, range, devices, PageWidgetDeviceRole.DEFAULT);
    }

    private void validateRankingSources(List<Device> devices, String pointName, String type) {
        Set<Integer> ids = devices.stream().map(Device::getId).collect(Collectors.toSet());
        var matches = sourceCatalog.availableSources(ids).stream()
                .filter(source -> ids.contains(source.deviceId()) && !source.ambiguous()
                        && pointName.equals(source.pointName()) && type.equalsIgnoreCase(source.dataPointType())
                        && source.unit() != null && !source.unit().isBlank()).toList();
        if (matches.stream().map(DeviceMeasurementSourceCatalog.Source::deviceId).distinct().count() != ids.size()
                || matches.stream().map(source -> source.unit().trim().toLowerCase(java.util.Locale.ROOT)).distinct().count() != 1) {
            throw new IllegalArgumentException("순위는 모든 장비의 동일한 유형과 단위의 측정항목이 필요합니다");
        }
    }

    private AggregateWidgetResponse sumLastPower(
            PageWidget widget,
            PageWidgetOp preset,
            PageWidgetChartRangePreset rangePreset,
            QueryRanges.Range range,
            List<Device> devices,
            PageWidgetDeviceRole role
    ) {
        String pointName = requireAggregatePoint(widget);
        List<String> pointNames = List.of(pointName);
        String unit = resolveUnit(devices, pointNames);
        List<AggregateDeviceValueResponse> rows = new ArrayList<>();
        SumResult result = sumLastInRange(devices, range, role, rows, widget);
        Double value = result.hasValue ? result.sum : null;
        return response(widget, preset, rangePreset, range, value, unit, rows);
    }

    private SumResult sumLastInRange(
            List<Device> devices,
            QueryRanges.Range range,
            PageWidgetDeviceRole role,
            List<AggregateDeviceValueResponse> out,
            PageWidget widget
    ) {
        String pointName = requireAggregatePoint(widget);
        List<Integer> deviceIds = devices.stream().map(Device::getId).toList();
        Map<Integer, Device> deviceById = indexDevices(devices);
        List<LastPoint> lasts = pointQuery.findLastInRange(
                deviceIds, List.of(pointName), range.start(), range.end());
        double sum = 0;
        boolean hasValue = false;
        for (LastPoint point : lasts) {
            if (!pointName.equals(point.pointName())) {
                continue;
            }
            Device device = deviceById.get(point.deviceId());
            out.add(new AggregateDeviceValueResponse(
                    point.deviceId(),
                    device == null ? null : device.getName(),
                    role.wireValue(),
                    QueryValues.round2(point.value())
            ));
            sum += point.value();
            hasValue = true;
        }
        return new SumResult(sum, hasValue);
    }

    private static String requireAggregatePoint(PageWidget widget) {
        List<String> pointNames = widget.pointNames();
        if (pointNames.isEmpty()) {
            throw new IllegalArgumentException("pointNames is required for aggregate");
        }
        if (pointNames.size() != 1) {
            throw new IllegalArgumentException("aggregate supports exactly one pointName");
        }
        return pointNames.get(0);
    }

    private String resolveUnit(List<Device> devices, List<String> pointNames) {
        Set<Integer> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toSet());
        String unit = null;
        for (DeviceMeasurementSourceCatalog.Source point : sourceCatalog.availableSources(deviceIds)) {
            if (!deviceIds.contains(point.deviceId()) || point.ambiguous()
                    || !pointNames.contains(point.pointName())) {
                continue;
            }
            String candidate = blankToNull(point.unit());
            if (candidate == null) {
                continue;
            }
            if (unit == null) {
                unit = candidate;
            } else if (!unit.equalsIgnoreCase(candidate)) {
                return null;
            }
        }
        return unit;
    }

    private List<Device> resolveEnabledDevices(PageWidget widget, PageWidgetDeviceRole... roles) {
        Set<PageWidgetDeviceRole> allowed = Set.of(roles);
        List<Device> devices = new ArrayList<>();
        for (PageWidgetDevice mapping : widget.getDevices()) {
            if (!allowed.contains(mapping.getDeviceRole())) {
                continue;
            }
            Device device = mapping.getDevice();
            if (device.isEnabled()) {
                devices.add(device);
            }
        }
        if (allowed.contains(PageWidgetDeviceRole.DEFAULT) || allowed.contains(PageWidgetDeviceRole.TOTAL)) {
            for (Device device : widget.resolvedDefaultDevices()) {
                if (device.isEnabled() && devices.stream().noneMatch(existing -> existing.getId().equals(device.getId()))) {
                    devices.add(device);
                }
            }
        }
        if (devices.size() > MAX_DEVICE_IDS) {
            throw new IllegalArgumentException(
                    "widget maps to more than " + MAX_DEVICE_IDS + " enabled devices");
        }
        return devices;
    }

    private PageWidget findAggregateWidget(Integer widgetId) {
        PageWidget widget = PageWidgetFinder.findRequired(pageWidgetRepository, widgetId);
        if (widget.getQueryKind() != PageWidgetQueryKind.aggregate) {
            throw new IllegalArgumentException(
                    "widget queryKind must be aggregate, but was " + widget.getQueryKind());
        }
        if (!widget.isEnabled()) {
            throw new IllegalArgumentException("widget is disabled");
        }
        return widget;
    }

    private static PageWidgetChartRangePreset resolvePreset(
            PageWidget widget,
            PageWidgetOp preset,
            String override
    ) {
        if (override != null && !override.isBlank()) {
            return PageWidgetChartRangePreset.from(override);
        }
        PageWidgetChartRangePreset fromWidget = widget.getAggregateRangePreset();
        if (fromWidget != null) {
            return fromWidget;
        }
        return switch (preset) {
            case usage, usage_rank, demand -> PageWidgetChartRangePreset.today;
            case power, power_rank -> PageWidgetChartRangePreset.last_24h;
        };
    }

    private static Map<Integer, Device> indexDevices(List<Device> devices) {
        Map<Integer, Device> map = new HashMap<>();
        for (Device device : devices) {
            map.put(device.getId(), device);
        }
        return map;
    }

    private static Map<String, Double> indexByDevicePoint(List<LastPoint> points) {
        Map<String, Double> map = new HashMap<>();
        for (LastPoint point : points) {
            map.put(key(point.deviceId(), point.pointName()), point.value());
        }
        return map;
    }

    private static String key(Integer deviceId, String pointOrTime) {
        return deviceId + "|" + pointOrTime;
    }

    private static AggregateWidgetResponse empty(
            PageWidget widget,
            PageWidgetOp preset,
            PageWidgetChartRangePreset rangePreset,
            QueryRanges.Range range,
            String unit
    ) {
        return response(widget, preset, rangePreset, range, null, unit, List.of());
    }

    private static AggregateWidgetResponse response(
            PageWidget widget,
            PageWidgetOp preset,
            PageWidgetChartRangePreset rangePreset,
            QueryRanges.Range range,
            Double value,
            String unit,
            List<AggregateDeviceValueResponse> devices
    ) {
        return new AggregateWidgetResponse(
                widget.getId(),
                widget.getName(),
                widget.getPageCode().getCode(),
                preset.name(),
                rangePreset.name(),
                range.start(),
                range.end(),
                QueryValues.round2(value),
                unit,
                devices.size(),
                List.copyOf(devices),
                null
        );
    }

    private AggregateWidgetResponse withDataStatus(PageWidget widget, AggregateWidgetResponse response) {
        String pointName = widget.pointNames().isEmpty() ? null : widget.pointNames().get(0);
        List<Device> devices = resolveEnabledDevices(widget, PageWidgetDeviceRole.DEFAULT, PageWidgetDeviceRole.TOTAL);
        Map<String, LastPoint> latestBySource = new HashMap<>();
        if (pointName != null && !devices.isEmpty()) {
            List<Integer> deviceIds = devices.stream().map(Device::getId).toList();
            for (LastPoint point : pointQuery.findLast(deviceIds, List.of(pointName), Duration.ofHours(24))) {
                latestBySource.put(key(point.deviceId(), point.pointName()), point);
            }
        }
        List<Instant> collectedTimes = devices.stream()
                .map(device -> latestBySource.get(key(device.getId(), pointName)))
                .map(point -> point == null ? null : point.time())
                .toList();
        WidgetDataStatusResponse dataStatus = widgetDataStatusResolver
                .resolve(collectedTimes, widget.getDataFreshnessMinutes());
        return new AggregateWidgetResponse(
                response.widgetId(), response.widgetName(), response.pageCode(), response.aggregatePreset(),
                response.rangePreset(), response.start(), response.end(), response.value(), response.unit(),
                response.contributingDevices(), response.devices(), dataStatus, response.peakTime());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record SumResult(double sum, boolean hasValue) {
    }
}
