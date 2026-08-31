package net.vivans.dcim.module.live.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceCapabilityPointResponse;
import net.vivans.dcim.module.device.api.dto.DeviceCapabilityResponse;
import net.vivans.dcim.module.device.application.DeviceCapabilityQueryService;
import net.vivans.dcim.module.live.api.dto.LiveSelectionItemRequest;
import net.vivans.dcim.module.live.infrastructure.collector.LiveCollectionPointSpec;
import net.vivans.dcim.module.live.infrastructure.collector.LiveCollectionSpec;
import net.vivans.dcim.module.live.infrastructure.collector.LiveCollectionTargetSpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveTelemetrySpecService {

    static final int LIVE_INTERVAL_MS = 1000;
    private static final String SNMP_PROTOCOL = "snmp";
    private static final String DEFAULT_COMMUNITY = "public";
    private static final int DEFAULT_TIMEOUT_MS = 800;
    private static final int DEFAULT_RETRIES = 0;
    private static final int DEFAULT_MAX_CONCURRENCY = 10;

    private final DeviceCapabilityQueryService deviceCapabilityQueryService;

    public LiveCollectionSpec build(List<LiveSelectionItemRequest> items) {
        Map<Integer, DeviceCapabilityResponse> capabilities = capabilitiesByDeviceId();
        List<LiveCollectionTargetSpec> targets = new ArrayList<>();
        for (LiveSelectionItemRequest item : items) {
            DeviceCapabilityResponse capability = capabilities.get(item.deviceId());
            if (capability == null || capability.endpoint() == null) {
                throw new IllegalArgumentException("device is not selectable for live SNMP: " + item.deviceId());
            }
            Map<String, DeviceCapabilityPointResponse> pointsByName = pointsByName(capability);
            List<LiveCollectionPointSpec> points = new ArrayList<>();
            for (String pointName : item.pointNames()) {
                DeviceCapabilityPointResponse point = pointsByName.get(pointName);
                if (point == null || point.resolvedOid() == null || point.resolvedOid().isBlank()) {
                    throw new IllegalArgumentException(
                            "unknown pointName '" + pointName + "' for device " + item.deviceId());
                }
                points.add(new LiveCollectionPointSpec(
                        point.name(),
                        point.oidTemplate(),
                        point.requiresInstance(),
                        point.scale(),
                        point.unit()
                ));
            }
            targets.add(new LiveCollectionTargetSpec(
                    capability.deviceId(),
                    capability.deviceName(),
                    capability.endpoint().host(),
                    capability.endpoint().port(),
                    capability.endpoint().instanceId(),
                    List.copyOf(points)
            ));
        }
        return new LiveCollectionSpec(
                LIVE_INTERVAL_MS,
                SNMP_PROTOCOL,
                DEFAULT_COMMUNITY,
                DEFAULT_TIMEOUT_MS,
                DEFAULT_RETRIES,
                DEFAULT_MAX_CONCURRENCY,
                List.copyOf(targets)
        );
    }

    private Map<Integer, DeviceCapabilityResponse> capabilitiesByDeviceId() {
        Map<Integer, DeviceCapabilityResponse> byId = new LinkedHashMap<>();
        for (DeviceCapabilityResponse capability : deviceCapabilityQueryService.getCapabilities(null, null, null)) {
            byId.put(capability.deviceId(), capability);
        }
        return byId;
    }

    private static Map<String, DeviceCapabilityPointResponse> pointsByName(DeviceCapabilityResponse capability) {
        Map<String, DeviceCapabilityPointResponse> byName = new LinkedHashMap<>();
        for (DeviceCapabilityPointResponse point : capability.points()) {
            byName.put(point.name(), point);
        }
        return byName;
    }
}
