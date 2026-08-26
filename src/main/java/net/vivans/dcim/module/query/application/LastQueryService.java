package net.vivans.dcim.module.query.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetDevice;
import net.vivans.dcim.module.device.domain.model.PageWidgetPoint;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.query.api.dto.LastDeviceResponse;
import net.vivans.dcim.module.query.api.dto.LastPointValueResponse;
import net.vivans.dcim.module.query.api.dto.LastWidgetResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
public class LastQueryService {

    static final int DEFAULT_LOOKBACK_HOURS = 24;
    static final int MAX_LOOKBACK_HOURS = 168;
    static final int MAX_DEVICE_IDS = 200;

    private final PageWidgetRepository pageWidgetRepository;
    private final PointQuery pointQuery;
    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;

    public LastWidgetResponse getLast(Integer widgetId, Integer lookbackHours) {
        PageWidget widget = findLastWidget(widgetId);
        List<String> pointNames = resolvePointNames(widget);
        Duration lookback = resolveLookback(lookbackHours);

        if (pointNames.isEmpty()) {
            return emptyResponse(widget);
        }

        List<Device> devices = resolveDevices(widget);
        if (devices.isEmpty()) {
            return emptyResponse(widget);
        }

        List<Integer> deviceIds = devices.stream().map(Device::getId).toList();
        Map<Integer, Device> deviceById = devices.stream()
                .collect(Collectors.toMap(Device::getId, device -> device, (a, b) -> a, LinkedHashMap::new));
        Map<Integer, Map<String, String>> unitsByModelId = resolveUnitsByModelId(devices);

        List<LastPoint> points = pointQuery.findLast(deviceIds, pointNames, lookback);
        Map<Integer, List<LastPoint>> pointsByDevice = new LinkedHashMap<>();
        for (LastPoint point : points) {
            pointsByDevice.computeIfAbsent(point.deviceId(), ignored -> new ArrayList<>()).add(point);
        }

        List<LastDeviceResponse> deviceResponses = new ArrayList<>();
        for (Map.Entry<Integer, Device> entry : deviceById.entrySet()) {
            List<LastPoint> devicePoints = pointsByDevice.get(entry.getKey());
            if (devicePoints == null || devicePoints.isEmpty()) {
                continue;
            }
            Device device = entry.getValue();
            Map<String, String> unitByPoint = unitsByModelId.getOrDefault(
                    device.getDeviceModel().getId(),
                    Map.of()
            );
            List<LastPointValueResponse> pointResponses = devicePoints.stream()
                    .sorted(Comparator.comparing(LastPoint::pointName))
                    .map(point -> new LastPointValueResponse(
                            point.pointName(),
                            unitByPoint.get(point.pointName()),
                            point.value(),
                            point.time()
                    ))
                    .toList();
            deviceResponses.add(LastDeviceResponse.from(device, pointResponses));
        }

        return new LastWidgetResponse(
                widget.getId(),
                widget.getName(),
                widget.getPageCode().getCode(),
                deviceResponses
        );
    }

    private Map<Integer, Map<String, String>> resolveUnitsByModelId(List<Device> devices) {
        Set<Integer> modelIds = devices.stream()
                .map(device -> device.getDeviceModel().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Integer, Map<String, String>> unitsByModelId = new HashMap<>();
        for (DeviceModelSnmpPoint point : deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(modelIds)) {
            Integer modelId = point.getModelProtocol().getDeviceModel().getId();
            unitsByModelId
                    .computeIfAbsent(modelId, ignored -> new HashMap<>())
                    .putIfAbsent(point.getName(), blankToNull(point.getUnit()));
        }
        return unitsByModelId;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static LastWidgetResponse emptyResponse(PageWidget widget) {
        return new LastWidgetResponse(
                widget.getId(),
                widget.getName(),
                widget.getPageCode().getCode(),
                List.of()
        );
    }

    private PageWidget findLastWidget(Integer widgetId) {
        if (widgetId == null) {
            throw new IllegalArgumentException("widgetId is required");
        }
        PageWidget widget = pageWidgetRepository.findById(widgetId)
                .orElseThrow(() -> new EntityNotFoundException("PageWidget not found: " + widgetId));
        if (widget.getQueryKind() != PageWidgetQueryKind.last) {
            throw new IllegalArgumentException(
                    "widget queryKind must be last, but was " + widget.getQueryKind());
        }
        if (!widget.isEnabled()) {
            throw new IllegalArgumentException("widget is disabled");
        }
        return widget;
    }

    private static List<String> resolvePointNames(PageWidget widget) {
        return widget.getPoints().stream()
                .map(PageWidgetPoint::getPointName)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }

    private List<Device> resolveDevices(PageWidget widget) {
        List<Device> devices = new ArrayList<>();
        for (PageWidgetDevice mapping : widget.getDevices()) {
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

    private static Duration resolveLookback(Integer lookbackHours) {
        int hours = lookbackHours == null ? DEFAULT_LOOKBACK_HOURS : lookbackHours;
        if (hours < 1 || hours > MAX_LOOKBACK_HOURS) {
            throw new IllegalArgumentException(
                    "lookbackHours must be between 1 and " + MAX_LOOKBACK_HOURS);
        }
        return Duration.ofHours(hours);
    }
}
