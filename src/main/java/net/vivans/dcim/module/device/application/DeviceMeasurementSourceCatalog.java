package net.vivans.dcim.module.device.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceEndpointModbusRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceModbusBitFieldRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelModbusPointRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Values are catalogued by their InfluxDB destination device, not the polling endpoint. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceMeasurementSourceCatalog {
    private final DeviceRepository deviceRepository;
    private final DeviceModelSnmpPointRepository snmpPointRepository;
    private final DeviceModelModbusPointRepository modbusPointRepository;
    private final DeviceModbusReadingRepository modbusReadingRepository;
    private final DeviceProtocolEndpointRepository endpointRepository;
    private final DeviceEndpointModbusRepository modbusEndpointRepository;
    private final DeviceModbusBitFieldRepository bitFieldRepository;
    private final DeviceModelMqttPointRepository mqttPointRepository;
    private final DeviceMqttIdentityRepository mqttIdentityRepository;

    public record Source(Integer deviceId, String deviceName, Integer modelId, String protocol,
                         String pointName, String unit, String dataPointType,
                         Integer sourceDeviceId, String origin, boolean ambiguous,
                         Integer dataPointTypeId, boolean thresholdSupported, String phase,
                         String phaseLabel, Integer phaseNotationOrder, String phaseNotationCode) {
        public Source(Integer deviceId, String deviceName, Integer modelId, String protocol,
                      String pointName, String unit, String dataPointType, Integer sourceDeviceId,
                      String origin, boolean ambiguous, Integer dataPointTypeId, boolean thresholdSupported) {
            this(deviceId, deviceName, modelId, protocol, pointName, unit, dataPointType,
                    sourceDeviceId, origin, ambiguous, dataPointTypeId, thresholdSupported, null, null, null, null);
        }

        public Source(Integer deviceId, String deviceName, Integer modelId, String protocol,
                      String pointName, String unit, String dataPointType, Integer sourceDeviceId,
                      String origin, boolean ambiguous, Integer dataPointTypeId, boolean thresholdSupported,
                      String phase) {
            this(deviceId, deviceName, modelId, protocol, pointName, unit, dataPointType, sourceDeviceId,
                    origin, ambiguous, dataPointTypeId, thresholdSupported, phase, null, null, null);
        }

        public Source(Integer deviceId, String deviceName, Integer modelId, String protocol,
                      String pointName, String unit, String dataPointType,
                      Integer sourceDeviceId, String origin, boolean ambiguous) {
            this(deviceId, deviceName, modelId, protocol, pointName, unit, dataPointType,
                    sourceDeviceId, origin, ambiguous, null, false, null, null, null, null);
        }

        public Source(Integer deviceId, String deviceName, Integer modelId, String protocol,
                      String pointName, String unit, String dataPointType, Integer sourceDeviceId,
                      String origin, boolean ambiguous, Integer dataPointTypeId, boolean thresholdSupported,
                      String phase, String phaseLabel, Integer phaseNotationOrder) {
            this(deviceId, deviceName, modelId, protocol, pointName, unit, dataPointType, sourceDeviceId,
                    origin, ambiguous, dataPointTypeId, thresholdSupported, phase, phaseLabel,
                    phaseNotationOrder, null);
        }
    }

    public List<Source> availableSources() {
        return availableSources(null);
    }

    public List<Source> availableSources(Set<Integer> deviceIds) {
        List<Source> all = new ArrayList<>();
        for (Device device : deviceRepository.findAllEnabled()) {
            int deviceId = device.getId();
            if (deviceIds != null && !deviceIds.contains(deviceId)) continue;
            int modelId = device.getDeviceModel().getId();
            var endpoints = endpointRepository.findAllByDeviceIdOrderByIdAsc(deviceId);
            if (endpoints.stream().anyMatch(endpoint -> endpoint.isEnabled()
                    && "snmp".equalsIgnoreCase(endpoint.getProtocolType().getCode()))) {
                snmpPointRepository.findAllEnabledByDeviceModelIds(Set.of(modelId)).forEach(point ->
                        all.add(source(device, "snmp", point.getName(), point.getUnit(), point.getDataPointType(), deviceId, "MODEL_POINT", point.getPhase())));
            }
            modbusReadingRepository.findAllByTargetDeviceIdOrderByIdAsc(deviceId).stream()
                    .filter(reading -> reading.isEnabled() && reading.getPoint().isEnabled()
                            && reading.getEndpointModbus().getEndpoint().isEnabled())
                    .forEach(reading -> {
                        int pollingDeviceId = reading.getEndpointModbus().getEndpoint().getDevice().getId();
                        all.add(source(device, "modbus", reading.getPointName(), reading.getPoint().getUnit(), reading.getPoint().getDataPointType(), pollingDeviceId, "READING", reading.getPoint().getPhase()));
                        bitFieldRepository.findAllByReading_IdOrderByIdAsc(reading.getId()).forEach(field ->
                                all.add(source(device, "modbus", field.getPointName(), null,
                                        null, pollingDeviceId, "BIT_FIELD")));
                    });
            if (endpoints.stream().anyMatch(endpoint -> endpoint.isEnabled()
                    && "modbus".equalsIgnoreCase(endpoint.getProtocolType().getCode())
                    && modbusEndpointRepository.findByEndpointId(endpoint.getId())
                        .map(modbus -> modbus.getUnitId() != null).orElse(false))) {
                modbusPointRepository.findAllEnabledByDeviceModelIds(Set.of(modelId)).stream()
                        .filter(point -> !point.isRequiresInstance())
                        .forEach(point -> all.add(source(device, "modbus", point.getName(), point.getUnit(),
                                point.getDataPointType(), deviceId, "MODEL_POINT", point.getPhase())));
            }
            if (mqttIdentityRepository.findAllByDeviceId(deviceId).stream().anyMatch(identity -> identity.isEnabled())) {
                mqttPointRepository.findAllEnabledByDeviceModelIdIn(Set.of(modelId)).stream()
                        .filter(point -> point.getComponentScope().name().equals("DEVICE"))
                        .forEach(point -> all.add(source(device, "mqtt", point.getPointName(), point.getUnit(),
                                point.getDataPointType(), deviceId, "MODEL_POINT", point.getPhase())));
            }
        }
        Map<String, Source> unique = new LinkedHashMap<>();
        for (Source source : all) {
            String key = source.deviceId() + "\u0000" + source.protocol() + "\u0000" + source.pointName();
            unique.putIfAbsent(key, source);
        }
        Map<String, Set<String>> protocolsByValue = new LinkedHashMap<>();
        for (Source source : unique.values()) {
            String key = source.deviceId() + "\u0000" + source.pointName();
            protocolsByValue.computeIfAbsent(key, ignored -> new java.util.HashSet<>()).add(source.protocol());
        }
        return unique.values().stream().map(source -> {
            boolean ambiguous = protocolsByValue.get(source.deviceId() + "\u0000" + source.pointName()).size() > 1;
            return new Source(source.deviceId(), source.deviceName(), source.modelId(), source.protocol(),
                    source.pointName(), source.unit(), source.dataPointType(), source.sourceDeviceId(),
                    source.origin(), ambiguous, source.dataPointTypeId(), source.thresholdSupported(), source.phase(),
                    source.phaseLabel(), source.phaseNotationOrder(), source.phaseNotationCode());
        }).sorted(Comparator.comparing(Source::deviceName).thenComparing(Source::pointName)).toList();
    }

    private static Source source(Device device, String protocol, String name, String unit,
                                 net.vivans.dcim.module.common.domain.model.CommonCode dataPointType,
                                 Integer sourceDeviceId, String origin) {
        return source(device, protocol, name, unit, dataPointType, sourceDeviceId, origin, (String) null);
    }

    private static Source source(Device device, String protocol, String name, String unit,
                                 net.vivans.dcim.module.common.domain.model.CommonCode dataPointType,
                                 Integer sourceDeviceId, String origin, String phase) {
        return source(device, protocol, name, unit, dataPointType, sourceDeviceId, origin, phase, null, null, null);
    }

    private static Source source(Device device, String protocol, String name, String unit,
                                 net.vivans.dcim.module.common.domain.model.CommonCode dataPointType,
                                 Integer sourceDeviceId, String origin,
                                 net.vivans.dcim.module.common.domain.model.CommonCode phase) {
        var model = device.getDeviceModel();
        String phaseCode = phase == null ? null : phase.getCode();
        Integer notationOrder = model.getPhaseNotation() == null || model.getPhaseNotation().getSortOrder() == null
                ? 1 : model.getPhaseNotation().getSortOrder();
        String notationCode = model.getPhaseNotation() == null ? "L123" : model.getPhaseNotation().getCode();
        return source(device, protocol, name, unit, dataPointType, sourceDeviceId, origin, phaseCode,
                model.phaseLabel(phaseCode), notationOrder, notationCode);
    }

    private static Source source(Device device, String protocol, String name, String unit,
                                 net.vivans.dcim.module.common.domain.model.CommonCode dataPointType,
                                 Integer sourceDeviceId, String origin, String phase,
                                 String phaseLabel, Integer phaseNotationOrder, String phaseNotationCode) {
        boolean thresholdSupported = dataPointType != null
                && "DATA_POINT_TYPE".equals(dataPointType.getCodeGroup().getGroupKey())
                && dataPointType.isThresholdSupported();
        return new Source(device.getId(), device.getName(), device.getDeviceModel().getId(), protocol,
                name, unit, dataPointType == null ? null : dataPointType.getCode(), sourceDeviceId, origin,
                false, dataPointType == null ? null : dataPointType.getId(), thresholdSupported, phase,
                phaseLabel, phaseNotationOrder, phaseNotationCode);
    }

}
