package net.vivans.dcim.module.live.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceCapabilityPointResponse;
import net.vivans.dcim.module.device.api.dto.DeviceCapabilityResponse;
import net.vivans.dcim.module.device.application.DeviceCapabilityQueryService;
import net.vivans.dcim.module.live.api.dto.LiveDeviceResponse;
import net.vivans.dcim.module.live.api.dto.LivePointResponse;
import net.vivans.dcim.module.live.api.dto.LiveSelectionItemRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveTelemetryQueryService {

    private final DeviceCapabilityQueryService deviceCapabilityQueryService;

    public List<LiveDeviceResponse> getSelectableDevices() {
        List<LiveDeviceResponse> devices = new ArrayList<>();
        for (DeviceCapabilityResponse capability : deviceCapabilityQueryService.getCapabilities(null, null, null)) {
            LiveDeviceResponse device = toSelectableDevice(capability);
            if (device != null) {
                devices.add(device);
            }
        }
        return devices;
    }

    public List<LiveSelectionItemRequest> normalizeAndValidate(List<LiveSelectionItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<Integer, Set<String>> allowedPointsByDeviceId = allowedPointsByDeviceId();
        Set<Integer> seenDeviceIds = new LinkedHashSet<>();
        List<LiveSelectionItemRequest> normalized = new ArrayList<>();
        for (LiveSelectionItemRequest item : items) {
            if (item == null || item.deviceId() == null) {
                throw new IllegalArgumentException("deviceId is required");
            }
            if (!seenDeviceIds.add(item.deviceId())) {
                throw new IllegalArgumentException("duplicate deviceId in request: " + item.deviceId());
            }
            Set<String> allowedPoints = allowedPointsByDeviceId.get(item.deviceId());
            if (allowedPoints == null) {
                throw new IllegalArgumentException("device is not selectable for live SNMP: " + item.deviceId());
            }
            List<String> pointNames = normalizePointNames(item.pointNames(), item.deviceId(), allowedPoints);
            normalized.add(new LiveSelectionItemRequest(item.deviceId(), pointNames));
        }
        return List.copyOf(normalized);
    }

    private Map<Integer, Set<String>> allowedPointsByDeviceId() {
        Map<Integer, Set<String>> allowed = new LinkedHashMap<>();
        for (LiveDeviceResponse device : getSelectableDevices()) {
            Set<String> names = new LinkedHashSet<>();
            for (LivePointResponse point : device.points()) {
                names.add(point.name());
            }
            allowed.put(device.deviceId(), names);
        }
        return allowed;
    }

    private static List<String> normalizePointNames(
            List<String> rawNames,
            Integer deviceId,
            Set<String> allowedPoints
    ) {
        if (rawNames == null || rawNames.isEmpty()) {
            throw new IllegalArgumentException("pointNames must not be empty: deviceId=" + deviceId);
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String rawName : rawNames) {
            if (rawName == null || rawName.isBlank()) {
                throw new IllegalArgumentException("pointName must not be blank: deviceId=" + deviceId);
            }
            String pointName = rawName.trim();
            if (!allowedPoints.contains(pointName)) {
                throw new IllegalArgumentException(
                        "unknown pointName '" + pointName + "' for device " + deviceId);
            }
            unique.add(pointName);
        }
        return List.copyOf(unique);
    }

    private static LiveDeviceResponse toSelectableDevice(DeviceCapabilityResponse capability) {
        if (capability.endpoint() == null) {
            return null;
        }
        List<LivePointResponse> points = new ArrayList<>();
        for (DeviceCapabilityPointResponse point : capability.points()) {
            if (point.resolvedOid() == null || point.resolvedOid().isBlank()) {
                continue;
            }
            points.add(new LivePointResponse(point.name(), point.unit()));
        }
        if (points.isEmpty()) {
            return null;
        }
        return new LiveDeviceResponse(
                capability.deviceId(),
                capability.deviceName(),
                capability.locationNodeName(),
                capability.modelId(),
                capability.modelName(),
                List.copyOf(points)
        );
    }
}
