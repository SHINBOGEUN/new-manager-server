package net.vivans.dcim.module.query.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.device.domain.model.PageWidgetDevice;
import net.vivans.dcim.module.device.domain.model.PageWidgetDeviceRole;
import net.vivans.dcim.module.device.domain.model.PageWidgetOp;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.query.api.dto.AggregateDeviceValueResponse;
import net.vivans.dcim.module.query.api.dto.AggregateWidgetResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;

    public AggregateWidgetResponse getAggregate(Integer widgetId, String rangePresetOverride) {
        PageWidget widget = findAggregateWidget(widgetId);
        PageWidgetOp preset = widget.getOp();
        if (preset == null) {
            throw new IllegalArgumentException("aggregatePreset/op is required");
        }
        PageWidgetChartRangePreset rangePreset = resolvePreset(widget, preset, rangePresetOverride);
        QueryRanges.Range range = QueryRanges.resolve(rangePreset);

        return switch (preset) {
            case usage -> computeUsage(widget, preset, rangePreset, range);
            case power -> computePower(widget, preset, rangePreset, range);
        };
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
        return sumLastPower(widget, preset, rangePreset, range, devices, PageWidgetDeviceRole.DEFAULT);
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
        if (devices.size() > MAX_DEVICE_IDS) {
            throw new IllegalArgumentException(
                    "widget maps to more than " + MAX_DEVICE_IDS + " enabled devices");
        }
        return devices;
    }

    private PageWidget findAggregateWidget(Integer widgetId) {
        if (widgetId == null) {
            throw new IllegalArgumentException("widgetId is required");
        }
        PageWidget widget = pageWidgetRepository.findById(widgetId)
                .orElseThrow(() -> new EntityNotFoundException("PageWidget not found: " + widgetId));
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
            case usage -> PageWidgetChartRangePreset.today;
            case power -> PageWidgetChartRangePreset.last_24h;
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
                List.copyOf(devices)
        );
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
