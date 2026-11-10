package net.vivans.dcim.module.live.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceCapabilityPointResponse;
import net.vivans.dcim.module.device.api.dto.DeviceCapabilityResponse;
import net.vivans.dcim.module.device.application.DeviceCapabilityQueryService;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.collectortask.application.CollectionGroupSpecService;
import net.vivans.dcim.module.collectortask.application.CollectionGroupModbusSpec;
import net.vivans.dcim.module.live.api.dto.LiveDeviceResponse;
import net.vivans.dcim.module.live.api.dto.LivePointResponse;
import net.vivans.dcim.module.live.api.dto.LiveSelectionItemRequest;
import net.vivans.dcim.module.live.infrastructure.collector.LiveCollectionPointSpec;
import net.vivans.dcim.module.live.infrastructure.collector.LiveCollectionSpec;
import net.vivans.dcim.module.live.infrastructure.collector.LiveCollectionTargetSpec;
import net.vivans.dcim.module.live.infrastructure.collector.LiveModbusTargetSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveTelemetrySpecService {

    @Value("${live.telemetry.interval-ms:1000}")
    private int intervalMs = 1000;
    @Value("${collection.snmp.community:public}")
    private String community = "public";
    @Value("${live.telemetry.timeout-ms:800}")
    private int timeoutMs = 800;
    @Value("${live.telemetry.retries:0}")
    private int retries = 0;
    @Value("${live.telemetry.max-concurrency:10}")
    private int maxConcurrency = 10;

    private final DeviceCapabilityQueryService deviceCapabilityQueryService;
    private final DeviceRepository deviceRepository;
    private final CollectionGroupSpecService collectionGroupSpecService;
    private final LiveTelemetryQueryService liveTelemetryQueryService;

    public LiveCollectionSpec build(List<LiveSelectionItemRequest> items) {
        Map<Integer, DeviceCapabilityResponse> capabilities = capabilitiesByDeviceId();
        Map<Integer, LiveDeviceResponse> selectable = new LinkedHashMap<>();
        for (LiveDeviceResponse device : liveTelemetryQueryService.getSelectableDevices()) {
            selectable.put(device.deviceId(), device);
        }
        List<LiveCollectionTargetSpec> targets = new ArrayList<>();
        List<LiveModbusTargetSpec> modbusTargets = new ArrayList<>();
        for (LiveSelectionItemRequest item : items) {
            if ("modbus".equals(item.protocol())) {
                appendModbusTargets(item, selectable.get(item.deviceId()), modbusTargets);
                continue;
            }
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
        String protocol = targets.isEmpty() ? "modbus" : modbusTargets.isEmpty() ? "snmp" : "mixed";
        return new LiveCollectionSpec(
                intervalMs,
                protocol,
                community,
                timeoutMs,
                retries,
                maxConcurrency,
                List.copyOf(targets),
                List.copyOf(modbusTargets)
        );
    }

    private void appendModbusTargets(LiveSelectionItemRequest item, LiveDeviceResponse device,
                                     List<LiveModbusTargetSpec> output) {
        if (device == null) throw new IllegalArgumentException("device is not selectable for live Modbus: " + item.deviceId());
        var source = deviceRepository.findById(item.sourceDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Modbus source device not found: " + item.sourceDeviceId()));
        var plan = collectionGroupSpecService.previewModbus(source);
        Set<String> selected = new LinkedHashSet<>(item.pointNames());
        Set<String> found = new LinkedHashSet<>();
        Map<String, String> units = new LinkedHashMap<>();
        for (LivePointResponse point : device.points()) {
            if ("modbus".equals(point.protocol()) && item.sourceDeviceId().equals(point.sourceDeviceId())
                    && selected.contains(point.name())) units.put(point.name(), point.unit());
        }
        for (CollectionGroupModbusSpec.ModbusTarget target : plan.targets()) {
            if (!item.deviceId().equals(target.deviceId())) continue;
            List<CollectionGroupModbusSpec.ModbusPoint> points = new ArrayList<>();
            for (CollectionGroupModbusSpec.ModbusPoint point : target.points()) {
                var bits = point.bitFields() == null ? List.<CollectionGroupModbusSpec.ModbusBitField>of()
                        : point.bitFields().stream().filter(bit -> selected.contains(bit.name())).toList();
                if (!selected.contains(point.name()) && bits.isEmpty()) continue;
                if (selected.contains(point.name())) found.add(point.name());
                bits.forEach(bit -> found.add(bit.name()));
                points.add(new CollectionGroupModbusSpec.ModbusPoint(point.name(), point.registerType(),
                        point.address(), point.dataType(), point.byteOrder(), point.scale(), point.offset(), bits));
            }
            if (!points.isEmpty()) {
                output.add(new LiveModbusTargetSpec(device.deviceName(), item.sourceDeviceId(),
                        new CollectionGroupModbusSpec.ModbusTarget(target.deviceId(), target.host(),
                                target.port(), target.unitId(), List.copyOf(points)),
                        java.util.Collections.unmodifiableMap(new LinkedHashMap<>(units))));
            }
        }
        if (!found.containsAll(selected)) {
            throw new IllegalArgumentException("Modbus points changed while building live selection: " + item.deviceId());
        }
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
