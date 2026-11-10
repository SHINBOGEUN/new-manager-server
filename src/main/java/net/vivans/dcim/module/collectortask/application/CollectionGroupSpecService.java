package net.vivans.dcim.module.collectortask.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskDevice;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceEndpointModbus;
import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;
import net.vivans.dcim.module.device.domain.model.DeviceModbusBitField;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceModbusBitFieldRepository;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.model.DeviceSnmpInstance;
import net.vivans.dcim.module.device.domain.repository.DeviceEndpointModbusRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceSnmpInstanceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusRegisterType;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusDataType;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelModbusPointRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionGroupSpecService {

    public static final String SNMP_PROTOCOL_CODE = "snmp";
    public static final String MODBUS_PROTOCOL_CODE = "modbus";
    public static boolean isCollectorProtocol(String code) {
        return SNMP_PROTOCOL_CODE.equalsIgnoreCase(code) || MODBUS_PROTOCOL_CODE.equalsIgnoreCase(code);
    }
    /** 장비 모델이 수집 작업(Task) 모델과 다를 때 남기는 skip 사유 마커. 프로토콜에 관계없이 공통으로 사용한다. */
    public static final String MODEL_MISMATCH_MARKER = "model mismatch";
    @Value("${collection.snmp.community:public}")
    private String community = "public";
    @Value("${collection.timeout-ms:2000}")
    private int timeoutMs = 2000;
    @Value("${collection.retries:1}")
    private int retries = 1;
    @Value("${collection.max-concurrency:10}")
    private int maxConcurrency = 10;

    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;
    private final DeviceModelModbusPointRepository deviceModelModbusPointRepository;
    private final DeviceProtocolEndpointRepository deviceProtocolEndpointRepository;
    private final DeviceEndpointModbusRepository deviceEndpointModbusRepository;
    private final DeviceModbusReadingRepository deviceModbusReadingRepository;
    private final DeviceModbusBitFieldRepository deviceModbusBitFieldRepository;
    private final DeviceSnmpInstanceRepository deviceSnmpInstanceRepository;
    private final ObjectMapper objectMapper;

    public String generateJson(CollectionTaskGroup group) {
        try {
            return objectMapper.writeValueAsString(generate(group).payload());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize collection group spec", e);
        }
    }

    public CollectionGroupPlan generate(CollectionTaskGroup group) {
        String protocol = group.getTask().getScriptType().getCode();
        if (MODBUS_PROTOCOL_CODE.equalsIgnoreCase(protocol)) {
            return generateModbus(group);
        }
        CollectionGroupSpec spec = generateSnmp(group);
        Map<Integer, List<CollectionGroupPlan.PointSource>> pointsBySource = new LinkedHashMap<>();
        for (CollectionGroupTargetSpec target : spec.targets()) {
            List<CollectionGroupPlan.PointSource> sources = new ArrayList<>();
            for (CollectionGroupOidSpec oid : spec.oids()) {
                sources.add(new CollectionGroupPlan.PointSource(target.deviceId(), oid.name(), null));
            }
            if (!sources.isEmpty()) {
                pointsBySource.put(target.deviceId(), sources);
            }
        }
        return new CollectionGroupPlan(spec, spec.skipped(), pointsBySource);
    }

    private CollectionGroupSpec generateSnmp(CollectionTaskGroup group) {
        CollectionTask task = group.getTask();
        List<String> skipped = new ArrayList<>();

        if (!SNMP_PROTOCOL_CODE.equalsIgnoreCase(task.getScriptType().getCode())) {
            skipped.add("scriptType '" + task.getScriptType().getCode() + "' is not supported yet");
            return emptySpec(group, skipped);
        }

        DeviceModelProtocol snmpProtocol = findProtocol(task.getDeviceModel(), SNMP_PROTOCOL_CODE);
        if (snmpProtocol == null) {
            skipped.add("model has no SNMP protocol");
            return emptySpec(group, skipped);
        }

        List<CollectionGroupOidSpec> oids = new ArrayList<>();
        boolean requiresAnyInstance = false;
        for (DeviceModelSnmpPoint point : deviceModelSnmpPointRepository
                .findAllByModelProtocolIdOrderByIdAsc(snmpProtocol.getId())) {
            if (!point.isEnabled()) {
                skipped.add("point '" + point.getName() + "' skipped: disabled");
                continue;
            }
            oids.add(new CollectionGroupOidSpec(
                    point.getName(),
                    point.getOid(),
                    point.isRequiresInstance(),
                    point.getScale()
            ));
            if (point.isRequiresInstance()) {
                requiresAnyInstance = true;
            }
        }
        if (oids.isEmpty()) {
            skipped.add("no collectible SNMP points");
        }

        List<CollectionGroupTargetSpec> targets = new ArrayList<>();
        if (!oids.isEmpty()) {
            DeviceModel taskModel = task.getDeviceModel();
            for (CollectionTaskDevice mapping : group.getDevices()) {
                Device device = mapping.getDevice();
                // 프로토콜(SNMP/Modbus 등)과 무관하게, 장비 모델이 이 수집 작업의 모델과 다르면
                // endpoint 유무를 따지기 전에 먼저 걸러내고 원인을 명확히 남긴다.
                if (!taskModel.getId().equals(device.getDeviceModel().getId())) {
                    skipped.add(modelMismatchReason(device, taskModel));
                    continue;
                }
                CollectionGroupTargetSpec target = toTarget(device, requiresAnyInstance, skipped);
                if (target != null) {
                    targets.add(target);
                }
            }
        }
        if (targets.isEmpty()) {
            skipped.add("no collectible devices in group");
        }

        return new CollectionGroupSpec(
                task.getId(),
                group.getId(),
                task.getDeviceModel().getId(),
                SNMP_PROTOCOL_CODE,
                group.getCronExpression(),
                community,
                timeoutMs,
                retries,
                maxConcurrency,
                oids,
                targets,
                skipped
        );
    }

    private static String modelMismatchReason(Device device, DeviceModel expectedModel) {
        DeviceModel actualModel = device.getDeviceModel();
        return "device:" + device.getId() + " " + device.getName() + " - " + MODEL_MISMATCH_MARKER
                + " (expected modelId=" + expectedModel.getId() + " '" + expectedModel.getName() + "'"
                + ", actual modelId=" + actualModel.getId() + " '" + actualModel.getName() + "')";
    }

    private CollectionGroupTargetSpec toTarget(Device device, boolean requiresAnyInstance, List<String> skipped) {
        if (!device.isEnabled()) {
            skipped.add("device:" + device.getId() + " " + device.getName() + " - disabled");
            return null;
        }
        DeviceProtocolEndpoint endpoint = findEnabledEndpoint(device.getId(), SNMP_PROTOCOL_CODE);
        if (endpoint == null) {
            skipped.add("device:" + device.getId() + " " + device.getName() + " - no enabled SNMP endpoint");
            return null;
        }
        Integer instanceId = deviceSnmpInstanceRepository.findByEndpointId(endpoint.getId())
                .map(DeviceSnmpInstance::getInstanceId)
                .orElse(null);
        if (requiresAnyInstance && instanceId == null) {
            skipped.add("device:" + device.getId() + " " + device.getName() + " - missing SNMP instance");
            return null;
        }
        return new CollectionGroupTargetSpec(device.getId(), endpoint.getHost(), endpoint.getPort(), instanceId);
    }

    private DeviceProtocolEndpoint findEnabledEndpoint(Integer deviceId, String protocolCode) {
        for (DeviceProtocolEndpoint endpoint : deviceProtocolEndpointRepository.findAllByDeviceIdOrderByIdAsc(deviceId)) {
            if (endpoint.isEnabled() && protocolCode.equalsIgnoreCase(endpoint.getProtocolType().getCode())) {
                return endpoint;
            }
        }
        return null;
    }

    private static DeviceModelProtocol findProtocol(DeviceModel deviceModel, String protocolCode) {
        for (DeviceModelProtocol protocol : deviceModel.getProtocols()) {
            if (protocolCode.equalsIgnoreCase(protocol.getProtocolType().getCode())) {
                return protocol;
            }
        }
        return null;
    }

    private CollectionGroupSpec emptySpec(CollectionTaskGroup group, List<String> skipped) {
        return new CollectionGroupSpec(
                group.getTask().getId(),
                group.getId(),
                group.getTask().getDeviceModel().getId(),
                group.getTask().getScriptType().getCode(),
                group.getCronExpression(),
                community,
                timeoutMs,
                retries,
                maxConcurrency,
                List.of(),
                List.of(),
                skipped
        );
    }

    private CollectionGroupPlan generateModbus(CollectionTaskGroup group) {
        CollectionTask task = group.getTask();
        List<String> skipped = new ArrayList<>();
        Map<Integer, List<CollectionGroupPlan.PointSource>> pointsBySource = new LinkedHashMap<>();
        Map<TargetKey, List<CollectionGroupModbusSpec.ModbusPoint>> pointsByTarget = new LinkedHashMap<>();
        Map<Integer, Set<String>> usedNamesByStorageDevice = new HashMap<>();
        DeviceModelProtocol protocol = findProtocol(task.getDeviceModel(), MODBUS_PROTOCOL_CODE);

        if (protocol == null) {
            skipped.add("model has no Modbus protocol");
        } else {
            List<DeviceModelModbusPoint> modelPoints = deviceModelModbusPointRepository
                    .findAllByModelProtocolIdOrderByIdAsc(protocol.getId()).stream()
                    .filter(DeviceModelModbusPoint::isEnabled)
                    .toList();
            if (modelPoints.isEmpty()) {
                skipped.add("no collectible Modbus points");
            }

            for (CollectionTaskDevice mapping : group.getDevices()) {
                Device source = mapping.getDevice();
                if (!task.getDeviceModel().getId().equals(source.getDeviceModel().getId())) {
                    skipped.add(modelMismatchReason(source, task.getDeviceModel()));
                    continue;
                }
                appendModbusSource(source, protocol, modelPoints, pointsByTarget,
                        usedNamesByStorageDevice, pointsBySource, skipped);
            }
        }

        List<CollectionGroupModbusSpec.ModbusTarget> targets = toModbusTargets(pointsByTarget);
        if (targets.isEmpty()) {
            skipped.add("no collectible devices in group");
        }
        CollectionGroupModbusSpec spec = new CollectionGroupModbusSpec(
                task.getId(), group.getId(), task.getDeviceModel().getId(), MODBUS_PROTOCOL_CODE,
                group.getCronExpression(), timeoutMs, retries, maxConcurrency,
                List.of(), targets, skipped);
        return new CollectionGroupPlan(spec, skipped, pointsBySource);
    }

    /** 정기 수집과 동일한 규칙으로 한 원본 장비의 읽기 계획을 만든다. 등록/저장은 하지 않는다. */
    public ModbusPreviewPlan previewModbus(Device source) {
        DeviceModelProtocol protocol = findProtocol(source.getDeviceModel(), MODBUS_PROTOCOL_CODE);
        if (protocol == null) {
            throw new IllegalArgumentException("device model has no Modbus protocol");
        }
        List<DeviceModelModbusPoint> modelPoints = deviceModelModbusPointRepository
                .findAllByModelProtocolIdOrderByIdAsc(protocol.getId()).stream()
                .filter(DeviceModelModbusPoint::isEnabled).toList();
        Map<TargetKey, List<CollectionGroupModbusSpec.ModbusPoint>> pointsByTarget = new LinkedHashMap<>();
        Map<Integer, Set<String>> usedNamesByStorageDevice = new HashMap<>();
        Map<Integer, List<CollectionGroupPlan.PointSource>> pointsBySource = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();
        appendModbusSource(source, protocol, modelPoints, pointsByTarget,
                usedNamesByStorageDevice, pointsBySource, skipped);
        return new ModbusPreviewPlan(toModbusTargets(pointsByTarget), skipped);
    }

    public record ModbusPreviewPlan(List<CollectionGroupModbusSpec.ModbusTarget> targets, List<String> skipped) {
    }

    private static List<CollectionGroupModbusSpec.ModbusTarget> toModbusTargets(
            Map<TargetKey, List<CollectionGroupModbusSpec.ModbusPoint>> pointsByTarget) {
        List<CollectionGroupModbusSpec.ModbusTarget> targets = new ArrayList<>();
        pointsByTarget.forEach((key, points) -> targets.add(new CollectionGroupModbusSpec.ModbusTarget(
                key.storageDeviceId(), key.host(), key.port(), key.unitId(), List.copyOf(points))));
        return targets;
    }

    private void appendModbusSource(
            Device source, DeviceModelProtocol protocol, List<DeviceModelModbusPoint> modelPoints,
            Map<TargetKey, List<CollectionGroupModbusSpec.ModbusPoint>> pointsByTarget,
            Map<Integer, Set<String>> usedNamesByStorageDevice,
            Map<Integer, List<CollectionGroupPlan.PointSource>> pointsBySource, List<String> skipped) {
        if (!source.isEnabled()) {
            skipped.add(deviceReason(source, "disabled"));
            return;
        }
        DeviceProtocolEndpoint endpoint = findEnabledEndpoint(source.getId(), MODBUS_PROTOCOL_CODE);
        if (endpoint == null) {
            skipped.add(deviceReason(source, "no enabled Modbus endpoint"));
            return;
        }
        DeviceEndpointModbus endpointModbus = deviceEndpointModbusRepository.findByEndpointId(endpoint.getId())
                .orElse(null);
        if (endpointModbus == null) {
            skipped.add(deviceReason(source, "missing Modbus endpoint settings"));
            return;
        }

        List<CollectionGroupPlan.PointSource> sourcePoints = new ArrayList<>();
        for (DeviceModelModbusPoint point : modelPoints) {
            if (point.isRequiresInstance()) continue;
            if (endpointModbus.getUnitId() == null) {
                skipped.add(deviceReason(source, "missing Modbus unitId for fixed point '" + point.getName() + "'"));
                continue;
            }
            addModbusPoint(source, source, endpoint, endpointModbus.getUnitId(), point.getName(),
                    point.getAddress(), point, List.of(), pointsByTarget, usedNamesByStorageDevice, sourcePoints, skipped);
        }
        for (DeviceModbusReading reading : deviceModbusReadingRepository.findAllByEndpointIdOrderByIdAsc(endpoint.getId())) {
            if (!reading.isEnabled()) continue;
            DeviceModelModbusPoint point = reading.getPoint();
            if (!point.isEnabled() || !point.isRequiresInstance()
                    || !protocol.getId().equals(point.getModelProtocol().getId())) {
                skipped.add(deviceReason(source, "invalid Modbus reading '" + reading.getPointName() + "'"));
                continue;
            }
            Device storageDevice = reading.getTargetDevice();
            if (!storageDevice.isEnabled()) {
                skipped.add(deviceReason(source, "target device " + storageDevice.getId() + " disabled"));
                continue;
            }
            addModbusPoint(source, storageDevice, endpoint, reading.getUnitId(), reading.getPointName(),
                    reading.getAddress(), point,
                    deviceModbusBitFieldRepository.findAllByReading_IdOrderByIdAsc(reading.getId()),
                    pointsByTarget, usedNamesByStorageDevice, sourcePoints, skipped);
        }
        if (sourcePoints.isEmpty()) skipped.add(deviceReason(source, "no collectible Modbus points"));
        else pointsBySource.put(source.getId(), sourcePoints);
    }

    private static void addModbusPoint(
            Device source,
            Device storageDevice,
            DeviceProtocolEndpoint endpoint,
            int unitId,
            String name,
            Integer address,
            DeviceModelModbusPoint point,
            List<DeviceModbusBitField> bitFields,
            Map<TargetKey, List<CollectionGroupModbusSpec.ModbusPoint>> pointsByTarget,
            Map<Integer, Set<String>> usedNamesByStorageDevice,
            List<CollectionGroupPlan.PointSource> sourcePoints,
            List<String> skipped
    ) {
        boolean bitRead = point.getRegisterType() == ModbusRegisterType.COIL
                || point.getRegisterType() == ModbusRegisterType.DISCRETE;
        int addressSpan = bitRead ? 1 : point.getDataType().getRegisterCount();
        if (!validPointName(name) || address == null || address < 0 || address > 65535
                || (long) address + addressSpan - 1 > 65535
                || point.getScale() != null && !Double.isFinite(point.getScale())
                || point.getOffset() != null && !Double.isFinite(point.getOffset())
                || bitRead && point.getScale() != null && point.getScale() != 1.0
                || !bitRead && point.getDataType().isMultiRegister() && point.getByteOrder() == null
                || !bitRead && !point.getDataType().isMultiRegister() && point.getByteOrder() != null) {
            skipped.add(deviceReason(source, "invalid Modbus point '" + name + "'"));
            return;
        }
        Set<String> names = usedNamesByStorageDevice.computeIfAbsent(storageDevice.getId(), ignored -> new HashSet<>());
        if (!names.add(name)) {
            skipped.add(deviceReason(source, "duplicate Modbus point '" + name
                    + "' for target device " + storageDevice.getId()));
            return;
        }
        List<CollectionGroupModbusSpec.ModbusBitField> derived = new ArrayList<>();
        for (DeviceModbusBitField field : bitFields) {
            if (bitRead || point.getDataType() == ModbusDataType.FLOAT32
                    || point.getScale() != null && point.getScale() != 1.0
                    || point.getOffset() != null && point.getOffset() != 0.0
                    || field.getBitOffset() < 0 || field.getBitWidth() < 1 || field.getBitWidth() > 32
                    || (long) field.getBitOffset() + field.getBitWidth() > point.getDataType().getRegisterCount() * 16) {
                skipped.add(deviceReason(source, "invalid Modbus bit field '" + field.getPointName() + "'"));
                continue;
            }
            if (!validPointName(field.getPointName()) || !names.add(field.getPointName())) {
                skipped.add(deviceReason(source, "duplicate or invalid derived Modbus point '"
                        + field.getPointName() + "' for target device " + storageDevice.getId()));
                continue;
            }
            derived.add(new CollectionGroupModbusSpec.ModbusBitField(field.getPointName(),
                    field.getBitOffset(), field.getBitWidth(), field.getValueMap(), field.getUnmappedValue()));
            sourcePoints.add(new CollectionGroupPlan.PointSource(storageDevice.getId(), field.getPointName(), null));
        }
        CollectionGroupModbusSpec.ModbusPoint specPoint = new CollectionGroupModbusSpec.ModbusPoint(
                name, point.getRegisterType().name(), address,
                bitRead ? "UINT16" : point.getDataType().name(),
                bitRead || point.getByteOrder() == null ? null : point.getByteOrder().name(),
                bitRead ? null : point.getScale(), bitRead ? null : point.getOffset(), derived);
        TargetKey key = new TargetKey(storageDevice.getId(), endpoint.getHost(), endpoint.getPort(), unitId);
        pointsByTarget.computeIfAbsent(key, ignored -> new ArrayList<>()).add(specPoint);
        sourcePoints.add(new CollectionGroupPlan.PointSource(storageDevice.getId(), name, point.getUnit()));
    }

    private static boolean validPointName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isWhitespace(ch) || Character.isSpaceChar(ch) || ch == ',' || ch == '=' || ch == '"') {
                return false;
            }
        }
        return true;
    }

    private static String deviceReason(Device device, String reason) {
        return "device:" + device.getId() + " " + device.getName() + " - " + reason;
    }

    private record TargetKey(Integer storageDeviceId, String host, int port, int unitId) {
    }
}
