package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.DevicePageMeasurementsResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DevicePageCodes;
import net.vivans.dcim.module.device.domain.model.DevicePageDevice;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceModbusBitFieldRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DevicePageDeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelModbusPointRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import net.vivans.dcim.module.mqtt.domain.model.MqttPointScope;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
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
public class DevicePageMeasurementService {
    private static final List<String> PROTOCOLS = List.of("snmp", "modbus", "mqtt");

    private final CommonCodeRepository commonCodeRepository;
    private final DeviceRepository deviceRepository;
    private final DevicePageDeviceRepository pageDeviceRepository;
    private final DeviceModelSnmpPointRepository snmpPointRepository;
    private final DeviceModelModbusPointRepository modbusPointRepository;
    private final DeviceModbusReadingRepository modbusReadingRepository;
    private final DeviceModbusBitFieldRepository bitFieldRepository;
    private final DeviceModelMqttPointRepository mqttPointRepository;
    private final PointQuery pointQuery;

    @Transactional
    public List<Integer> replaceDevices(String pageCode, List<Integer> deviceIds) {
        CommonCode page = findPage(pageCode);
        Set<Integer> selected = new LinkedHashSet<>(deviceIds);
        List<Device> devices = new ArrayList<>();
        for (Integer id : selected) {
            devices.add(deviceRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Device not found: " + id)));
        }
        List<DevicePageDevice> existing = pageDeviceRepository.findAllByPageCode_IdOrderByIdAsc(page.getId());
        List<DevicePageDevice> removed = existing.stream()
                .filter(mapping -> !selected.contains(mapping.getDevice().getId())).toList();
        pageDeviceRepository.deleteAll(removed);
        Set<Integer> linked = existing.stream().map(mapping -> mapping.getDevice().getId())
                .collect(Collectors.toSet());
        for (Device device : devices) {
            if (!linked.contains(device.getId())) {
                pageDeviceRepository.save(DevicePageDevice.create(page, device));
            }
        }
        return List.copyOf(selected);
    }

    public List<Integer> getDeviceIds(String pageCode) {
        CommonCode page = findPage(pageCode);
        return pageDeviceRepository.findAllByPageCode_IdOrderByIdAsc(page.getId()).stream()
                .map(mapping -> mapping.getDevice().getId())
                .toList();
    }

    public DevicePageMeasurementsResponse getMeasurements(String pageCode, int lookbackHours) {
        if (lookbackHours < 1 || lookbackHours > 720) {
            throw new IllegalArgumentException("lookbackHours must be between 1 and 720");
        }
        CommonCode page = findPage(pageCode);
        List<Device> devices = pageDeviceRepository.findAllByPageCode_IdOrderByIdAsc(page.getId()).stream()
                .map(DevicePageDevice::getDevice).toList();
        Map<Integer, LinkedHashMap<String, PointDefinition>> definitions = pointDefinitions(devices);
        Map<String, Map<String, LastPoint>> values = latestValues(definitions, lookbackHours);

        List<DevicePageMeasurementsResponse.DeviceMeasurement> results = new ArrayList<>();
        for (Device device : devices) {
            List<DevicePageMeasurementsResponse.PointMeasurement> points = new ArrayList<>();
            for (PointDefinition definition : definitions.get(device.getId()).values()) {
                LastPoint latest = values.getOrDefault(definition.protocol(), Map.of())
                        .get(valueKey(device.getId(), definition.pointName()));
                points.add(new DevicePageMeasurementsResponse.PointMeasurement(
                        definition.pointName(), definition.protocol(), definition.unit(),
                        definition.dataPointType(), latest == null ? null : latest.value(),
                        latest == null ? null : latest.time()));
            }
            results.add(new DevicePageMeasurementsResponse.DeviceMeasurement(
                    device.getId(), device.getName(), device.getDeviceModel().getId(),
                    device.getDeviceModel().getName(), device.getLocationNode().getCode(),
                    device.getLocationNode().getName(), device.isEnabled(), points));
        }
        return new DevicePageMeasurementsResponse(page.getCode(), page.getName(), lookbackHours, results);
    }

    private CommonCode findPage(String pageCode) {
        if (pageCode == null || pageCode.isBlank()) {
            throw new IllegalArgumentException("pageCode is required");
        }
        return commonCodeRepository.findByCodeGroupGroupKeyAndCode(
                        DevicePageCodes.DEVICE_PAGE_GROUP_KEY, pageCode.trim())
                .orElseThrow(() -> new EntityNotFoundException("DEVICE_PAGE code not found: " + pageCode.trim()));
    }

    private Map<Integer, LinkedHashMap<String, PointDefinition>> pointDefinitions(List<Device> devices) {
        Map<Integer, LinkedHashMap<String, PointDefinition>> result = new LinkedHashMap<>();
        if (devices.isEmpty()) return result;
        Set<Integer> modelIds = devices.stream().map(device -> device.getDeviceModel().getId())
                .collect(Collectors.toSet());
        Map<Integer, List<Device>> devicesByModel = devices.stream()
                .collect(Collectors.groupingBy(device -> device.getDeviceModel().getId()));
        for (Device device : devices) result.put(device.getId(), new LinkedHashMap<>());

        snmpPointRepository.findAllEnabledByDeviceModelIds(modelIds).forEach(point -> {
            int modelId = point.getModelProtocol().getDeviceModel().getId();
            for (Device device : devicesByModel.getOrDefault(modelId, List.of())) {
                add(result, device.getId(), new PointDefinition(point.getName(), "snmp", point.getUnit(),
                        point.getDataPointType() == null ? null : point.getDataPointType().getCode()));
            }
        });

        var readings = modbusReadingRepository.findAllByTargetDeviceIds(result.keySet()).stream()
                .filter(reading -> reading.isEnabled() && reading.getPoint().isEnabled()).toList();
        if (!readings.isEmpty()) {
            var bits = bitFieldRepository.findAllByReading_IdInOrderByIdAsc(
                    readings.stream().map(reading -> reading.getId()).toList());
            Map<Integer, Integer> targetByReading = new HashMap<>();
            for (var reading : readings) {
                int targetId = reading.getTargetDevice().getId();
                targetByReading.put(reading.getId(), targetId);
                add(result, targetId, new PointDefinition(reading.getPointName(), "modbus",
                        reading.getPoint().getUnit(), null));
            }
            for (var bit : bits) {
                add(result, targetByReading.get(bit.getReading().getId()),
                        new PointDefinition(bit.getPointName(), "modbus", null, "STATUS"));
            }
        }
        modbusPointRepository.findAllEnabledByDeviceModelIds(modelIds).stream()
                .filter(point -> !point.isRequiresInstance()).forEach(point -> {
                    int modelId = point.getModelProtocol().getDeviceModel().getId();
                    for (Device device : devicesByModel.getOrDefault(modelId, List.of())) {
                        add(result, device.getId(), new PointDefinition(point.getName(), "modbus", point.getUnit(), null));
                    }
                });

        // 구성요소별 최신값/차트 응답은 별도 GPU 조회 단계에서 구현한다.
        mqttPointRepository.findAllEnabledByDeviceModelIdIn(modelIds).stream()
                .filter(point -> point.getComponentScope() == MqttPointScope.DEVICE)
                .forEach(point -> {
            int modelId = point.getDeviceModel().getId();
            for (Device device : devicesByModel.getOrDefault(modelId, List.of())) {
                add(result, device.getId(), new PointDefinition(point.getPointName(), "mqtt", point.getUnit(),
                        point.getDataPointType().getCode()));
            }
        });
        return result;
    }

    private Map<String, Map<String, LastPoint>> latestValues(
            Map<Integer, LinkedHashMap<String, PointDefinition>> definitions, int lookbackHours) {
        Map<String, Map<String, LastPoint>> result = new HashMap<>();
        for (String protocol : PROTOCOLS) {
            List<Integer> deviceIds = new ArrayList<>();
            Set<String> pointNames = new LinkedHashSet<>();
            for (var entry : definitions.entrySet()) {
                boolean hasProtocol = false;
                for (PointDefinition point : entry.getValue().values()) {
                    if (!protocol.equals(point.protocol())) continue;
                    hasProtocol = true;
                    pointNames.add(point.pointName());
                }
                if (hasProtocol) deviceIds.add(entry.getKey());
            }
            if (deviceIds.isEmpty()) continue;
            Map<String, LastPoint> byPoint = new HashMap<>();
            for (LastPoint point : pointQuery.findLast(deviceIds, List.copyOf(pointNames),
                    Duration.ofHours(lookbackHours), protocol)) {
                byPoint.put(valueKey(point.deviceId(), point.pointName()), point);
            }
            result.put(protocol, byPoint);
        }
        return result;
    }

    private static void add(Map<Integer, LinkedHashMap<String, PointDefinition>> result,
                            Integer deviceId, PointDefinition point) {
        result.get(deviceId).putIfAbsent(point.protocol() + "\u0000" + point.pointName(), point);
    }

    private static String valueKey(Integer deviceId, String pointName) {
        return deviceId + "\u0000" + pointName;
    }

    private record PointDefinition(String pointName, String protocol, String unit, String dataPointType) {
    }
}
