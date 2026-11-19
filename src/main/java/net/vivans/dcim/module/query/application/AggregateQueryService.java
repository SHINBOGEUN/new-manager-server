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
    private final UnitNormalizer unitNormalizer;

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

    private Map<Integer, String> selectedSources(PageWidget widget, List<Device> devices) {
        Set<Integer> ids = devices.stream().map(Device::getId).collect(Collectors.toSet());
        Map<Integer, String> selected = new java.util.LinkedHashMap<>();
        for (var source : widget.aggregateSourceDefinitions()) {
            if (!ids.contains(source.device().getId())) continue;
            if (source.pointNames().size() != 1 || selected.putIfAbsent(source.device().getId(), source.pointNames().get(0)) != null)
                throw new IllegalArgumentException("장비마다 집계할 측정항목 하나가 필요합니다");
        }
        if (selected.size() != ids.size()) throw new IllegalArgumentException("일부 장비의 집계 측정항목이 없습니다");
        return selected;
    }

    private UnitNormalizer.Plan aggregatePlan(Map<Integer, String> selected) {
        UnitNormalizer.Plan all = unitNormalizer.plan(selected.keySet(), selected.values());
        Map<String, UnitNormalizer.Conversion> matching = new java.util.LinkedHashMap<>();
        selected.forEach((id, name) -> {
            String key = UnitNormalizer.key(id, name);
            if (all.conversions().containsKey(key)) matching.put(key, all.conversions().get(key));
        });
        UnitNormalizer.Plan plan = new UnitNormalizer.Plan(matching);
        if (matching.size() != selected.size() || plan.units().size() > 1
                || (plan.units().size() == 1 && matching.values().stream().anyMatch(value -> value.unit() == null)))
            throw new IllegalArgumentException("단위가 달라 합계를 낼 수 없습니다");
        return plan;
    }

    private void validateRankingSources(Map<Integer, String> selected, String type) {
        var matches = sourceCatalog.availableSources(selected.keySet()).stream()
                .filter(source -> !source.ambiguous() && source.pointName().equals(selected.get(source.deviceId()))
                        && type.equalsIgnoreCase(source.dataPointType()) && source.unit() != null && !source.unit().isBlank()).toList();
        if (matches.stream().map(DeviceMeasurementSourceCatalog.Source::deviceId).distinct().count() != selected.size()
                || aggregatePlan(selected).units().size() != 1)
            throw new IllegalArgumentException("모든 장비의 측정항목 유형과 단위를 확인하세요");
    }

    private AggregateWidgetResponse computeDemand(PageWidget widget, PageWidgetOp preset,
            PageWidgetChartRangePreset rangePreset, QueryRanges.Range range) {
        List<Device> devices = resolveEnabledDevices(widget, PageWidgetDeviceRole.DEFAULT, PageWidgetDeviceRole.TOTAL);
        if (devices.isEmpty()) return empty(widget, preset, rangePreset, range, null);
        Map<Integer, String> selected = selectedSources(widget, devices);
        validateRankingSources(selected, "POWER");
        UnitNormalizer.Plan plan = aggregatePlan(selected);
        List<Integer> ids = devices.stream().map(Device::getId).toList();
        List<String> names = selected.values().stream().distinct().toList();
        var points = pointQuery.findSeries(ids, names, range.start(), range.end(), "15m").stream()
                .filter(point -> point.pointName().equals(selected.get(point.deviceId()))).map(plan::point).toList();
        var peak = PowerDemandCalculator.peak(points, ids, range.start(), range.end());
        String unit = plan.units().isEmpty() ? null : plan.units().get(0);
        if (peak == null) return empty(widget, preset, rangePreset, range, unit);
        return new AggregateWidgetResponse(widget.getId(), widget.getName(), widget.getPageCode().getCode(),
                preset.name(), rangePreset.name(), range.start(), range.end(), QueryValues.round2(peak.value()), unit, ids.size(), List.of(), null, peak.time());
    }

    private AggregateWidgetResponse computeUsage(PageWidget widget, PageWidgetOp preset,
            PageWidgetChartRangePreset rangePreset, QueryRanges.Range range) {
        List<Device> devices = resolveEnabledDevices(widget, PageWidgetDeviceRole.DEFAULT, PageWidgetDeviceRole.TOTAL);
        if (devices.isEmpty()) return empty(widget, preset, rangePreset, range, null);
        Map<Integer, String> selected = selectedSources(widget, devices);
        if (preset == PageWidgetOp.usage_rank) validateRankingSources(selected, "ENERGY");
        UnitNormalizer.Plan plan = aggregatePlan(selected);
        List<Integer> ids = devices.stream().map(Device::getId).toList();
        List<String> names = selected.values().stream().distinct().toList();
        Map<String, Double> first = indexByDevicePoint(pointQuery.findFirstInRange(ids, names, range.start(), range.end()));
        Map<String, Double> last = indexByDevicePoint(pointQuery.findLastInRange(ids, names, range.start(), range.end()));
        String unit = plan.units().isEmpty() ? null : plan.units().get(0);
        List<AggregateDeviceValueResponse> rows = new ArrayList<>(); double sum = 0;
        for (Device device : devices) {
            String name = selected.get(device.getId()); Double f = first.get(key(device.getId(), name)), l = last.get(key(device.getId(), name));
            if (f == null || l == null || l < f) continue;
            double value = plan.value(device.getId(), name, l - f); var rounded = QueryValues.round2(value);
            UnitDisplay.Result display = UnitDisplay.of(rounded, unit);
            rows.add(new AggregateDeviceValueResponse(device.getId(), device.getName(), PageWidgetDeviceRole.DEFAULT.wireValue(), rounded, display.displayValue(), display.displayUnit()));
            sum += value;
        }
        return response(widget, preset, rangePreset, range, rows.isEmpty() ? null : sum, unit, rows);
    }

    private AggregateWidgetResponse computePower(PageWidget widget, PageWidgetOp preset,
            PageWidgetChartRangePreset rangePreset, QueryRanges.Range range) {
        List<Device> devices = resolveEnabledDevices(widget, PageWidgetDeviceRole.DEFAULT, PageWidgetDeviceRole.TOTAL);
        if (devices.isEmpty()) return empty(widget, preset, rangePreset, range, null);
        Map<Integer, String> selected = selectedSources(widget, devices);
        if (preset == PageWidgetOp.power_rank) validateRankingSources(selected, "POWER");
        UnitNormalizer.Plan plan = aggregatePlan(selected);
        List<Integer> ids = devices.stream().map(Device::getId).toList();
        List<String> names = selected.values().stream().distinct().toList();
        Map<Integer, Device> deviceById = indexDevices(devices);
        String unit = plan.units().isEmpty() ? null : plan.units().get(0);
        List<AggregateDeviceValueResponse> rows = new ArrayList<>(); double sum = 0;
        for (LastPoint point : pointQuery.findLastInRange(ids, names, range.start(), range.end())) {
            if (!point.pointName().equals(selected.get(point.deviceId()))) continue;
            double value = plan.value(point.deviceId(), point.pointName(), point.value()); var rounded = QueryValues.round2(value);
            UnitDisplay.Result display = UnitDisplay.of(rounded, unit); Device device = deviceById.get(point.deviceId());
            rows.add(new AggregateDeviceValueResponse(point.deviceId(), device == null ? null : device.getName(), PageWidgetDeviceRole.DEFAULT.wireValue(), rounded, display.displayValue(), display.displayUnit()));
            sum += value;
        }
        return response(widget, preset, rangePreset, range, rows.isEmpty() ? null : sum, unit, rows);
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
        List<Device> devices = resolveEnabledDevices(widget, PageWidgetDeviceRole.DEFAULT, PageWidgetDeviceRole.TOTAL);
        Map<Integer, String> selected = selectedSources(widget, devices);
        List<Integer> ids = devices.stream().map(Device::getId).toList();
        List<String> names = selected.values().stream().distinct().toList();
        Map<String, LastPoint> latest = new HashMap<>();
        if (!ids.isEmpty() && !names.isEmpty()) for (LastPoint point : pointQuery.findLast(ids, names, Duration.ofHours(24)))
            latest.put(key(point.deviceId(), point.pointName()), point);
        List<Instant> times = devices.stream().map(device -> latest.get(key(device.getId(), selected.get(device.getId()))))
                .map(point -> point == null ? null : point.time()).toList();
        WidgetDataStatusResponse status = widgetDataStatusResolver.resolve(times, widget.getDataFreshnessMinutes());
        return new AggregateWidgetResponse(response.widgetId(), response.widgetName(), response.pageCode(), response.aggregatePreset(),
                response.rangePreset(), response.start(), response.end(), response.value(), response.unit(), response.contributingDevices(),
                response.devices(), status, response.peakTime()).withUnitConverted(aggregatePlan(selected).converted());
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
