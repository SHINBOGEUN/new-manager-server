package net.vivans.dcim.module.devicemodel.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.devicemodel.api.dto.*;
import net.vivans.dcim.module.devicemodel.domain.model.*;
import net.vivans.dcim.module.devicemodel.domain.repository.*;
import net.vivans.dcim.module.lora.application.LoraValueMapValidator;
import net.vivans.dcim.module.mqtt.api.dto.MqttPointRequest;
import net.vivans.dcim.module.mqtt.application.MqttPointService;
import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;
import net.vivans.dcim.module.mqtt.domain.model.MqttPointScope;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceModelCatalogTransferService {
    public static final String FORMAT = "dcim-model-catalog";
    public static final int VERSION = 1;
    private static final int MAX_MODELS = 500;

    private final DeviceModelRepository modelRepository;
    private final DeviceModelSnmpPointRepository snmpRepository;
    private final DeviceModelModbusPointRepository modbusRepository;
    private final DeviceModelMqttPointRepository mqttRepository;
    private final CommonCodeRepository commonCodes;
    private final DeviceModelQueryService modelService;
    private final DeviceModelSnmpPointQueryService snmpService;
    private final DeviceModelModbusPointQueryService modbusService;
    private final MqttPointService mqttService;
    private final LoraValueMapValidator valueMapValidator;

    @Transactional(readOnly = true)
    public DeviceModelCatalogDocument exportCatalog(List<Integer> requestedIds) {
        List<DeviceModel> all = modelRepository.findAll(null, null);
        List<DeviceModel> selected;
        if (requestedIds == null || requestedIds.isEmpty()) {
            selected = all;
        } else {
            Set<Integer> ids = new LinkedHashSet<>(requestedIds);
            if (ids.size() != requestedIds.size() || ids.stream().anyMatch(id -> id == null || id <= 0)) {
                throw new IllegalArgumentException("ids must be unique positive integers");
            }
            Map<Integer, DeviceModel> byId = all.stream().collect(Collectors.toMap(DeviceModel::getId, Function.identity()));
            List<Integer> unknown = ids.stream().filter(id -> !byId.containsKey(id)).toList();
            if (!unknown.isEmpty()) throw new EntityNotFoundException("DeviceModel not found: " + unknown);
            selected = ids.stream().map(byId::get).toList();
        }
        List<DeviceModelCatalogDocument.Model> models = selected.stream().map(this::exportModel).toList();
        return new DeviceModelCatalogDocument(FORMAT, VERSION, Instant.now(), models);
    }

    @Transactional
    public DeviceModelCatalogImportReport importCatalog(DeviceModelCatalogDocument document, boolean dryRun, String conflictPolicy) {
        if (dryRun) TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        String policy = conflictPolicy == null || conflictPolicy.isBlank() ? "SKIP" : conflictPolicy.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SKIP", "UPDATE").contains(policy)) throw new IllegalArgumentException("onConflict must be SKIP or UPDATE");
        List<String> created = new ArrayList<>(), updated = new ArrayList<>(), skipped = new ArrayList<>();
        List<String> disabled = new ArrayList<>(), warnings = new ArrayList<>();
        List<DeviceModelCatalogImportReport.Error> errors = new ArrayList<>();
        validateDocument(document, errors);
        if (!errors.isEmpty()) return report(created, updated, skipped, disabled, warnings, errors);

        List<DeviceModel> existingModels = modelRepository.findAll(null, null);
        Set<String> inputKeys = new HashSet<>();
        List<PreparedModel> prepared = new ArrayList<>();
        for (int index = 0; index < document.models().size(); index++) {
            DeviceModelCatalogDocument.Model model = document.models().get(index);
            if (model == null) { errors.add(error("(model " + (index + 1) + ")", "model", "model entry must not be null")); continue; }
            String key = modelKey(model.manufacturer(), model.name());
            if (!inputKeys.add(key)) {
                errors.add(error(display(model), "model", "duplicate manufacturer + name in file"));
                continue;
            }
            List<DeviceModel> matches = existingModels.stream().filter(candidate -> modelKey(candidate.getManufacturer(), candidate.getName()).equals(key)).toList();
            if (matches.size() > 1) {
                errors.add(error(display(model), "model", "multiple existing models match manufacturer + name"));
                continue;
            }
            DeviceModel existing = matches.isEmpty() ? null : matches.get(0);
            if (existing != null && "SKIP".equals(policy)) {
                skipped.add(display(model) + " (already exists)");
                continue;
            }
            try {
                prepared.add(validateModel(model, existing));
                if (existing == null) created.add(display(model)); else updated.add(display(model));
            } catch (RuntimeException ex) {
                errors.add(error(display(model), "model", safeMessage(ex)));
            }
        }
        if (dryRun && errors.isEmpty()) previewUpdateEffects(prepared, disabled, warnings);
        if (!errors.isEmpty() || dryRun) return report(created, updated, skipped, disabled, warnings, errors);

        for (PreparedModel item : prepared) {
            applyModel(item, disabled, warnings);
        }
        return report(created, updated, skipped, disabled, warnings, errors);
    }

    private DeviceModelCatalogDocument.Model exportModel(DeviceModel model) {
        List<String> protocols = model.getSortedProtocols().stream().map(p -> p.getProtocolType().getCode().toLowerCase(Locale.ROOT)).toList();
        List<DeviceModelCatalogDocument.SnmpPoint> snmp = new ArrayList<>();
        List<DeviceModelCatalogDocument.ModbusPoint> modbus = new ArrayList<>();
        for (DeviceModelProtocol protocol : model.getSortedProtocols()) {
            String code = protocol.getProtocolType().getCode().toLowerCase(Locale.ROOT);
            if ("snmp".equals(code)) {
                for (DeviceModelSnmpPoint p : snmpRepository.findAllByModelProtocolIdOrderByIdAsc(protocol.getId())) {
                    snmp.add(new DeviceModelCatalogDocument.SnmpPoint(p.getName(), code(p.getDataPointType()), code(p.getCategoryCode()), code(p.getUnitCode()), p.getOid(), p.isRequiresInstance(), p.getScale(), p.isEnabled()));
                }
            } else if ("modbus".equals(code)) {
                for (DeviceModelModbusPoint p : modbusRepository.findAllByModelProtocolIdOrderByIdAsc(protocol.getId())) {
                    modbus.add(new DeviceModelCatalogDocument.ModbusPoint(p.getName(), code(p.getDataPointType()), code(p.getCategoryCode()), code(p.getUnitCode()), p.getRegisterType().name(), p.getDataType().name(), p.getByteOrder() == null ? null : p.getByteOrder().name(), p.getAddress(), p.isRequiresInstance(), p.getScale(), p.getOffset(), p.isEnabled()));
                }
            }
        }
        List<DeviceModelCatalogDocument.MqttPoint> mqtt = mqttRepository.findAllByDeviceModelIdOrderByIdAsc(model.getId()).stream()
                .map(p -> new DeviceModelCatalogDocument.MqttPoint(p.getPayloadField(), p.getPointName(), code(p.getDataPointType()), code(p.getCategoryCode()), code(p.getUnitCode()), p.getComponentScope().name(), p.getComponentType(), p.getComponentPath(), p.getComponentIndexField(), p.getComponentUuidField(), p.getScale(), p.getValueMap(), p.isEnabled()))
                .toList();
        return new DeviceModelCatalogDocument.Model(model.getName(), model.getManufacturer(), model.getDeviceType().getCode(), model.getDescription(), protocols, snmp, modbus, mqtt);
    }

    private PreparedModel validateModel(DeviceModelCatalogDocument.Model file, DeviceModel existing) {
        if (file.name() == null || file.name().isBlank() || file.manufacturer() == null || file.manufacturer().isBlank()) throw new IllegalArgumentException("name and manufacturer are required");
        if (file.description() != null && file.description().length() > 1000) throw new IllegalArgumentException("description exceeds 1000 characters");
        CommonCode type = requiredCode("MODEL_TYPE", file.deviceType(), "deviceType");
        DeviceModel target = existing == null ? DeviceModel.create(file.name(), file.manufacturer(), type, file.description()) : existing;
        if (file.protocols() == null || file.protocols().isEmpty()) throw new IllegalArgumentException("protocols must contain at least one protocol code");
        Map<String, CommonCode> protocols = new LinkedHashMap<>();
        for (String raw : file.protocols()) {
            String protocol = normalizeProtocol(raw);
            CommonCode code = requiredCode("PROTOCOL_TYPE", protocol, "protocols");
            if (protocols.putIfAbsent(protocol, code) != null) throw new IllegalArgumentException("duplicate protocol: " + protocol);
        }
        Map<String, DeviceModelProtocol> tempProtocols = new HashMap<>();
        for (Map.Entry<String, CommonCode> entry : protocols.entrySet()) {
            DeviceModelProtocol current = existing == null ? null : existing.getProtocols().stream().filter(p -> p.getProtocolType().getCode().equalsIgnoreCase(entry.getKey())).findFirst().orElse(null);
            tempProtocols.put(entry.getKey(), current == null ? DeviceModelProtocol.of(target, entry.getValue()) : current);
        }
        validateSnmp(file, tempProtocols.get("snmp"), existing);
        validateModbus(file, tempProtocols.get("modbus"), existing);
        validateMqtt(file, protocols.containsKey("mqtt") || "LORA_SENSOR".equalsIgnoreCase(type.getCode()), target, existing);
        if ((file.snmpPoints() != null && !file.snmpPoints().isEmpty()) && !protocols.containsKey("snmp")) throw new IllegalArgumentException("snmpPoints require snmp protocol");
        if ((file.modbusPoints() != null && !file.modbusPoints().isEmpty()) && !protocols.containsKey("modbus")) throw new IllegalArgumentException("modbusPoints require modbus protocol");
        if ((file.mqttPoints() != null && !file.mqttPoints().isEmpty()) && !protocols.containsKey("mqtt") && !"LORA_SENSOR".equalsIgnoreCase(type.getCode())) throw new IllegalArgumentException("mqttPoints require mqtt protocol");
        return new PreparedModel(file, existing, type, protocols);
    }

    private void validateSnmp(DeviceModelCatalogDocument.Model file, DeviceModelProtocol protocol, DeviceModel existing) {
        List<DeviceModelCatalogDocument.SnmpPoint> points = nullToEmpty(file.snmpPoints());
        Set<String> names = new HashSet<>(), oids = new HashSet<>();
        List<DeviceModelSnmpPoint> old = existingProtocol(existing, "snmp").map(p -> snmpRepository.findAllByModelProtocolIdOrderByIdAsc(p.getId())).orElse(List.of());
        for (var point : points) {
            CommonCode type = requiredCode("DATA_POINT_TYPE", point.dataPointType(), "snmpPoints.dataPointType");
            CommonCode category = optionalCode("CATEGORY", point.category(), "snmpPoints.category");
            CommonCode unit = optionalCode("UNIT", point.unit(), "snmpPoints.unit");
            if (point.name() == null || !names.add(point.name().trim().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("duplicate or blank SNMP point name: " + point.name());
            if (point.oid() == null || !oids.add(point.oid().trim())) throw new IllegalArgumentException("duplicate or blank SNMP OID: " + point.oid());
            if (protocol == null) throw new IllegalArgumentException("snmpPoints require snmp protocol");
            DeviceModelSnmpPoint.create(protocol, point.name(), point.oid(), Boolean.TRUE.equals(point.requiresInstance()), unit, point.scale(), !Boolean.FALSE.equals(point.enabled()), type, category);
            if (old.stream().anyMatch(p -> p.getOid().equals(point.oid()) && !p.getName().equalsIgnoreCase(point.name()))) throw new IllegalArgumentException("SNMP OID already exists");
        }
    }

    private void validateModbus(DeviceModelCatalogDocument.Model file, DeviceModelProtocol protocol, DeviceModel existing) {
        List<DeviceModelCatalogDocument.ModbusPoint> points = nullToEmpty(file.modbusPoints());
        Set<String> names = new HashSet<>();
        List<DeviceModelModbusPoint> old = existingProtocol(existing, "modbus").map(p -> modbusRepository.findAllByModelProtocolIdOrderByIdAsc(p.getId())).orElse(List.of());
        for (var point : points) {
            CommonCode type = requiredCode("DATA_POINT_TYPE", point.dataPointType(), "modbusPoints.dataPointType");
            CommonCode category = optionalCode("CATEGORY", point.category(), "modbusPoints.category");
            CommonCode unit = optionalCode("UNIT", point.unit(), "modbusPoints.unit");
            if (protocol == null) throw new IllegalArgumentException("modbusPoints require modbus protocol");
            if (point.name() == null || !names.add(point.name().trim().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("duplicate or blank Modbus point name: " + point.name());
            DeviceModelModbusPoint.create(protocol, point.name(), type, category, enumValue(ModbusRegisterType.class, point.registerType(), "registerType"), enumValue(ModbusDataType.class, point.dataType(), "dataType"), enumValue(ModbusByteOrder.class, point.byteOrder(), "byteOrder"), point.address(), Boolean.TRUE.equals(point.requiresInstance()), point.scale(), point.offset(), unit, !Boolean.FALSE.equals(point.enabled()));
            finite(point.scale(), "scale"); finite(point.offset(), "offset");
        }
    }

    private void validateMqtt(DeviceModelCatalogDocument.Model file, boolean mqttAllowed, DeviceModel target, DeviceModel existing) {
        List<DeviceModelCatalogDocument.MqttPoint> points = nullToEmpty(file.mqttPoints());
        Set<String> names = new HashSet<>(), fields = new HashSet<>();
        List<DeviceModelMqttPoint> old = existing == null ? List.of() : mqttRepository.findAllByDeviceModelIdOrderByIdAsc(existing.getId());
        for (var point : points) {
            if (!mqttAllowed) throw new IllegalArgumentException("mqttPoints require mqtt protocol or LORA_SENSOR device type");
            CommonCode type = requiredCode("DATA_POINT_TYPE", point.dataPointType(), "mqttPoints.dataPointType");
            CommonCode category = optionalCode("CATEGORY", point.category(), "mqttPoints.category");
            CommonCode unit = optionalCode("UNIT", point.unit(), "mqttPoints.unit");
            if (point.pointName() == null || !names.add(point.pointName().trim().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("duplicate or blank MQTT point name: " + point.pointName());
            String scope = point.componentScope() == null ? "DEVICE" : point.componentScope().trim().toUpperCase(Locale.ROOT);
            String path = point.componentPath() == null ? "" : point.componentPath().trim();
            if (!fields.add(path + "\u0000" + point.payloadField())) throw new IllegalArgumentException("duplicate MQTT payload path");
            MqttPointScope mqttScope = enumValue(MqttPointScope.class, scope, "componentScope");
            DeviceModelMqttPoint created = DeviceModelMqttPoint.create(target, point.payloadField(), point.pointName(), type, unit, point.scale(), point.valueMap(), !Boolean.FALSE.equals(point.enabled()), category);
            if (mqttScope == MqttPointScope.COMPONENT) created.configureComponent(point.componentType(), path, point.componentIndexField(), point.componentUuidField()); else created.configureDevice();
            finite(point.scale(), "scale");
            valueMapValidator.validate(point.valueMap());
            if (point.valueMap() != null && point.valueMap().length() > 1000) throw new IllegalArgumentException("valueMap exceeds 1000 characters");
            String importedPointName = point.pointName().trim();
            String identityPath = mqttScope == MqttPointScope.COMPONENT ? path : "";
            if (old.stream().anyMatch(p -> p.getComponentPath().equals(identityPath) && p.getPayloadField().equals(point.payloadField().trim()) && !p.getPointName().equalsIgnoreCase(importedPointName))) throw new IllegalArgumentException("MQTT payload path conflicts with another point");
        }
    }

    private void applyModel(PreparedModel item, List<String> disabled, List<String> warnings) {
        Integer modelId;
        if (item.existing() == null) {
            DeviceModelCreateRequest request = new DeviceModelCreateRequest(item.file().name(), item.file().manufacturer(), item.type().getId(), item.file().description(), item.protocols().values().stream().map(code -> new DeviceModelProtocolRequest(code.getId())).toList());
            modelId = modelService.createDeviceModel(request).id();
        } else {
            modelId = item.existing().getId();
            modelService.updateDeviceModelDetails(modelId, new DeviceModelDetailsUpdateRequest(item.file().name(), item.file().manufacturer(), item.type().getId(), item.file().description()));
            DeviceModel managed = modelRepository.findById(modelId).orElseThrow();
            Set<String> existingProtocols = managed.getProtocols().stream().map(p -> p.getProtocolType().getCode().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            for (Map.Entry<String, CommonCode> protocol : item.protocols().entrySet()) {
                if (!existingProtocols.contains(protocol.getKey())) managed.getProtocols().add(DeviceModelProtocol.of(managed, protocol.getValue()));
            }
            modelRepository.save(managed);
            modelRepository.flush();
            List<String> retained = existingProtocols.stream().filter(code -> !item.protocols().containsKey(code)).toList();
            if (!retained.isEmpty()) warnings.add(display(item.file()) + ": file에 없는 기존 프로토콜은 관계를 보존하고 해당 프로토콜 포인트를 비활성화했습니다: " + String.join(", ", retained));
        }
        DeviceModelResponse response = modelService.getDeviceModel(modelId);
        Map<String, Integer> protocolIds = response.protocols().stream().collect(Collectors.toMap(p -> p.protocolCode().toLowerCase(Locale.ROOT), DeviceModelProtocolResponse::id));
        for (Map.Entry<String, Integer> protocol : protocolIds.entrySet()) {
            if (item.protocols().containsKey(protocol.getKey())) continue;
            disableProtocolPoints(modelId, protocol.getKey(), protocol.getValue(), disabled);
        }
        mergeSnmp(item.file(), modelId, protocolIds.get("snmp"), disabled);
        mergeModbus(item.file(), modelId, protocolIds.get("modbus"), disabled);
        mergeMqtt(item.file(), modelId, disabled);
    }

    private void previewUpdateEffects(List<PreparedModel> prepared, List<String> disabled, List<String> warnings) {
        for (PreparedModel item : prepared) {
            DeviceModel existing = item.existing();
            if (existing == null) continue;
            for (DeviceModelProtocol protocol : existing.getProtocols()) {
                String code = protocol.getProtocolType().getCode().toLowerCase(Locale.ROOT);
                if (item.protocols().containsKey(code)) continue;
                warnings.add(display(item.file()) + ": file에 없는 기존 프로토콜은 유지되고 포인트를 비활성화합니다: " + code);
                if ("snmp".equals(code)) snmpRepository.findAllByModelProtocolIdOrderByIdAsc(protocol.getId()).stream().filter(DeviceModelSnmpPoint::isEnabled).forEach(p -> disabled.add(display(item.file()) + " · snmp · " + p.getName()));
                if ("modbus".equals(code)) modbusRepository.findAllByModelProtocolIdOrderByIdAsc(protocol.getId()).stream().filter(DeviceModelModbusPoint::isEnabled).forEach(p -> disabled.add(display(item.file()) + " · modbus · " + p.getName()));
            }
            existingProtocol(existing, "snmp").filter(p -> item.protocols().containsKey("snmp")).ifPresent(protocol -> {
                Set<String> incoming = nullToEmpty(item.file().snmpPoints()).stream().map(p -> p.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
                snmpRepository.findAllByModelProtocolIdOrderByIdAsc(protocol.getId()).stream().filter(p -> p.isEnabled() && !incoming.contains(p.getName().toLowerCase(Locale.ROOT))).forEach(p -> disabled.add(display(item.file()) + " · snmp · " + p.getName()));
            });
            existingProtocol(existing, "modbus").filter(p -> item.protocols().containsKey("modbus")).ifPresent(protocol -> {
                Set<String> incoming = nullToEmpty(item.file().modbusPoints()).stream().map(p -> p.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
                modbusRepository.findAllByModelProtocolIdOrderByIdAsc(protocol.getId()).stream().filter(p -> p.isEnabled() && !incoming.contains(p.getName().toLowerCase(Locale.ROOT))).forEach(p -> disabled.add(display(item.file()) + " · modbus · " + p.getName()));
            });
            Set<String> incomingMqtt = nullToEmpty(item.file().mqttPoints()).stream().map(p -> p.pointName().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            mqttRepository.findAllByDeviceModelIdOrderByIdAsc(existing.getId()).stream().filter(p -> p.isEnabled() && !incomingMqtt.contains(p.getPointName().toLowerCase(Locale.ROOT))).forEach(p -> disabled.add(display(item.file()) + " · mqtt · " + p.getPointName()));
        }
    }

    private void mergeSnmp(DeviceModelCatalogDocument.Model file, Integer modelId, Integer protocolId, List<String> disabled) {
        if (protocolId == null) return;
        List<DeviceModelSnmpPointResponse> current = snmpService.getDeviceModelSnmpPoints(modelId, protocolId);
        Map<String, DeviceModelSnmpPointResponse> byName = current.stream().collect(Collectors.toMap(p -> p.name().toLowerCase(Locale.ROOT), Function.identity()));
        Set<Integer> consumed = new HashSet<>();
        for (var p : nullToEmpty(file.snmpPoints())) {
            DeviceModelSnmpPointResponse old = byName.get(p.name().toLowerCase(Locale.ROOT));
            DeviceModelSnmpPointCreateRequest request = new DeviceModelSnmpPointCreateRequest(codeId("DATA_POINT_TYPE", p.dataPointType()), codeIdOrNull("CATEGORY", p.category()), codeIdOrNull("UNIT", p.unit()), p.name(), p.oid(), p.requiresInstance(), p.scale(), !Boolean.FALSE.equals(p.enabled()));
            if (old == null) snmpService.createDeviceModelSnmpPoint(modelId, protocolId, request);
            else { consumed.add(old.id()); snmpService.updateDeviceModelSnmpPoint(modelId, protocolId, old.id(), request); }
        }
        for (DeviceModelSnmpPointResponse p : current) if (!consumed.contains(p.id()) && p.enabled()) {
            snmpService.updateDeviceModelSnmpPoint(modelId, protocolId, p.id(), new DeviceModelSnmpPointCreateRequest(p.dataPointTypeId(), p.categoryCodeId(), p.unitCodeId(), p.name(), p.oid(), p.requiresInstance(), p.scale(), false));
            disabled.add(display(file) + " · snmp · " + p.name());
        }
    }

    private void mergeModbus(DeviceModelCatalogDocument.Model file, Integer modelId, Integer protocolId, List<String> disabled) {
        if (protocolId == null) return;
        List<DeviceModelModbusPointResponse> current = modbusService.getDeviceModelModbusPoints(modelId, protocolId);
        Map<String, DeviceModelModbusPointResponse> byName = current.stream().collect(Collectors.toMap(p -> p.name().toLowerCase(Locale.ROOT), Function.identity()));
        Set<Integer> consumed = new HashSet<>();
        for (var p : nullToEmpty(file.modbusPoints())) {
            DeviceModelModbusPointResponse old = byName.get(p.name().toLowerCase(Locale.ROOT));
            DeviceModelModbusPointCreateRequest request = new DeviceModelModbusPointCreateRequest(p.name(), codeId("DATA_POINT_TYPE", p.dataPointType()), codeIdOrNull("CATEGORY", p.category()), codeIdOrNull("UNIT", p.unit()), enumValue(ModbusRegisterType.class, p.registerType(), "registerType"), enumValue(ModbusDataType.class, p.dataType(), "dataType"), enumValue(ModbusByteOrder.class, p.byteOrder(), "byteOrder"), p.address(), p.requiresInstance(), p.scale(), p.offset(), !Boolean.FALSE.equals(p.enabled()));
            if (old == null) modbusService.createDeviceModelModbusPoint(modelId, protocolId, request);
            else { consumed.add(old.id()); modbusService.updateDeviceModelModbusPoint(modelId, protocolId, old.id(), request); }
        }
        for (DeviceModelModbusPointResponse p : current) if (!consumed.contains(p.id()) && p.enabled()) {
            modbusService.updateDeviceModelModbusPoint(modelId, protocolId, p.id(), new DeviceModelModbusPointCreateRequest(p.name(), p.dataPointTypeId(), p.categoryCodeId(), p.unitCodeId(), p.registerType(), p.dataType(), p.byteOrder(), p.address(), p.requiresInstance(), p.scale(), p.offset(), false));
            disabled.add(display(file) + " · modbus · " + p.name());
        }
    }

    private void mergeMqtt(DeviceModelCatalogDocument.Model file, Integer modelId, List<String> disabled) {
        List<DeviceModelMqttPoint> current = mqttRepository.findAllByDeviceModelIdOrderByIdAsc(modelId);
        Map<String, DeviceModelMqttPoint> byName = current.stream().collect(Collectors.toMap(p -> p.getPointName().toLowerCase(Locale.ROOT), Function.identity()));
        Set<Integer> consumed = new HashSet<>();
        for (var p : nullToEmpty(file.mqttPoints())) {
            DeviceModelMqttPoint old = byName.get(p.pointName().trim().toLowerCase(Locale.ROOT));
            MqttPointRequest request = new MqttPointRequest(p.payloadField(), p.pointName(), codeId("DATA_POINT_TYPE", p.dataPointType()), codeIdOrNull("CATEGORY", p.category()), codeIdOrNull("UNIT", p.unit()), p.scale(), p.valueMap(), !Boolean.FALSE.equals(p.enabled()), enumValue(MqttPointScope.class, p.componentScope() == null ? "DEVICE" : p.componentScope(), "componentScope"), p.componentType(), p.componentPath(), p.componentIndexField(), p.componentUuidField());
            if (old == null) mqttService.save(modelId, null, request);
            else { consumed.add(old.getId()); mqttService.save(modelId, old.getId(), request); }
        }
        for (DeviceModelMqttPoint p : current) if (!consumed.contains(p.getId()) && p.isEnabled()) {
            mqttService.save(modelId, p.getId(), new MqttPointRequest(p.getPayloadField(), p.getPointName(), p.getDataPointType().getId(), p.getCategoryCode() == null ? null : p.getCategoryCode().getId(), p.getUnitCode() == null ? null : p.getUnitCode().getId(), p.getScale(), p.getValueMap(), false, p.getComponentScope(), p.getComponentType(), p.getComponentPath(), p.getComponentIndexField(), p.getComponentUuidField()));
            disabled.add(display(file) + " · mqtt · " + p.getPointName());
        }
    }

    private void disableProtocolPoints(Integer modelId, String protocol, Integer protocolId, List<String> disabled) {
        if ("snmp".equals(protocol)) {
            for (var p : snmpService.getDeviceModelSnmpPoints(modelId, protocolId)) if (p.enabled()) {
                snmpService.updateDeviceModelSnmpPoint(modelId, protocolId, p.id(), new DeviceModelSnmpPointCreateRequest(p.dataPointTypeId(), p.categoryCodeId(), p.unitCodeId(), p.name(), p.oid(), p.requiresInstance(), p.scale(), false));
                disabled.add(displayModel(modelId) + " · snmp · " + p.name());
            }
        } else if ("modbus".equals(protocol)) {
            for (var p : modbusService.getDeviceModelModbusPoints(modelId, protocolId)) if (p.enabled()) {
                modbusService.updateDeviceModelModbusPoint(modelId, protocolId, p.id(), new DeviceModelModbusPointCreateRequest(p.name(), p.dataPointTypeId(), p.categoryCodeId(), p.unitCodeId(), p.registerType(), p.dataType(), p.byteOrder(), p.address(), p.requiresInstance(), p.scale(), p.offset(), false));
                disabled.add(displayModel(modelId) + " · modbus · " + p.name());
            }
        }
    }

    private String displayModel(Integer modelId) {
        return modelRepository.findById(modelId)
                .map(model -> model.getManufacturer() + " / " + model.getName())
                .orElse("model #" + modelId);
    }

    private void validateDocument(DeviceModelCatalogDocument document, List<DeviceModelCatalogImportReport.Error> errors) {
        if (document == null) { errors.add(error("(file)", "document", "JSON body is required")); return; }
        if (!FORMAT.equals(document.format())) errors.add(error("(file)", "format", "format must be " + FORMAT));
        if (document.version() != VERSION) errors.add(error("(file)", "version", "unsupported catalog version: " + document.version()));
        if (document.models() == null || document.models().isEmpty()) errors.add(error("(file)", "models", "models must not be empty"));
        else if (document.models().size() > MAX_MODELS) errors.add(error("(file)", "models", "at most " + MAX_MODELS + " models are allowed"));
    }

    private Optional<DeviceModelProtocol> existingProtocol(DeviceModel model, String code) {
        if (model == null) return Optional.empty();
        return model.getProtocols().stream().filter(p -> p.getProtocolType().getCode().equalsIgnoreCase(code)).findFirst();
    }
    private CommonCode requiredCode(String group, String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return commonCodes.findByCodeGroupGroupKeyAndCode(group, value.trim()).orElseThrow(() -> new EntityNotFoundException("missing code " + group + "/" + value));
    }
    private CommonCode optionalCode(String group, String value, String field) { return value == null || value.isBlank() ? null : requiredCode(group, value, field); }
    private Integer codeId(String group, String value) { return requiredCode(group, value, group).getId(); }
    private Integer codeIdOrNull(String group, String value) { return optionalCode(group, value, group) == null ? null : requiredCode(group, value, group).getId(); }
    private static String code(CommonCode value) { return value == null ? null : value.getCode(); }
    private static String normalizeProtocol(String value) {
        if (value == null) throw new IllegalArgumentException("protocol code is required");
        String code = value.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("snmp", "modbus", "mqtt").contains(code)) throw new IllegalArgumentException("unsupported protocol: " + value);
        return code;
    }
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("invalid " + field + ": " + value); }
    }
    private static void finite(Double value, String field) { if (value != null && !Double.isFinite(value)) throw new IllegalArgumentException(field + " must be finite"); }
    private static <T> List<T> nullToEmpty(List<T> value) { return value == null ? List.of() : value; }
    private static String modelKey(String manufacturer, String name) { return String.valueOf(manufacturer).trim().toLowerCase(Locale.ROOT) + "\u0000" + String.valueOf(name).trim().toLowerCase(Locale.ROOT); }
    private static String display(DeviceModelCatalogDocument.Model model) { return String.valueOf(model.manufacturer()) + " / " + String.valueOf(model.name()); }
    private static String safeMessage(RuntimeException ex) { return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(); }
    private static DeviceModelCatalogImportReport.Error error(String model, String field, String message) { return new DeviceModelCatalogImportReport.Error(model, field, message); }
    private static DeviceModelCatalogImportReport report(List<String> created, List<String> updated, List<String> skipped, List<String> disabled, List<String> warnings, List<DeviceModelCatalogImportReport.Error> errors) { return new DeviceModelCatalogImportReport(List.copyOf(created), List.copyOf(updated), List.copyOf(skipped), List.copyOf(disabled), List.copyOf(warnings), List.copyOf(errors)); }
    private record PreparedModel(DeviceModelCatalogDocument.Model file, DeviceModel existing, CommonCode type, Map<String, CommonCode> protocols) {}
}
