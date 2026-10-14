package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionScriptSyncService;
import net.vivans.dcim.module.collectortask.application.CollectionTaskService;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.device.api.dto.DeviceRegistrationOptionsResponse;
import net.vivans.dcim.module.device.api.dto.DeviceRegistrationRequest;
import net.vivans.dcim.module.device.api.dto.DeviceRegistrationResponse;
import net.vivans.dcim.module.device.api.dto.DeviceProtocolEndpointResponse;
import net.vivans.dcim.module.device.api.dto.DeviceModbusReadingCreateRequest;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelModbusPointRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointRequest;
import net.vivans.dcim.module.lora.application.DeviceLoraEndpointService;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceRegistrationService {

    private final DeviceModelRepository modelRepository;
    private final DeviceRepository deviceRepository;
    private final CollectionTaskRepository taskRepository;
    private final DeviceModelSnmpPointRepository snmpPoints;
    private final DeviceModelModbusPointRepository modbusPoints;
    private final DeviceModelMqttPointRepository loraPoints;
    private final DeviceQueryService deviceService;
    private final DeviceProtocolEndpointQueryService endpointService;
    private final DeviceSnmpInstanceQueryService snmpInstanceService;
    private final DeviceEndpointModbusQueryService modbusService;
    private final DeviceModbusReadingQueryService modbusReadingService;
    private final DeviceLoraEndpointService loraEndpointService;
    private final CollectionTaskService taskService;
    private final CollectionScriptSyncService scriptSyncService;

    public DeviceRegistrationOptionsResponse getOptions(Integer modelId) {
        DeviceModel model = findModel(modelId);
        List<CollectionTask> tasks = taskRepository.findAllByModelId(modelId);
        List<DeviceRegistrationOptionsResponse.ProtocolOption> protocols = model.getProtocols().stream()
                .map(protocol -> option(model, protocol, tasks)).toList();
        return new DeviceRegistrationOptionsResponse(modelId, model.getName(),
                model.getDeviceType().getCode(), protocols);
    }

    private DeviceRegistrationOptionsResponse.ProtocolOption option(
            DeviceModel model, DeviceModelProtocol protocol, List<CollectionTask> tasks) {
        String code = protocol.getProtocolType().getCode().toLowerCase();
        boolean lora = "mqtt".equals(code) && "LORA_SENSOR".equals(model.getDeviceType().getCode());
        int pointCount = switch (code) {
            case "snmp" -> snmpPoints.findAllByModelProtocolIdOrderByIdAsc(protocol.getId()).size();
            case "modbus" -> modbusPoints.findAllByModelProtocolIdOrderByIdAsc(protocol.getId()).size();
            case "mqtt" -> lora ? loraPoints.findAllByDeviceModelIdOrderByIdAsc(model.getId()).size() : 0;
            default -> 0;
        };
        List<DeviceRegistrationOptionsResponse.CollectionGroupOption> groups = tasks.stream()
                .filter(task -> ("snmp".equals(code) || "modbus".equals(code))
                        && task.isActive() && task.getScriptType().getId().equals(protocol.getProtocolType().getId()))
                .flatMap(task -> task.getGroups().stream().filter(CollectionTaskGroup::isActive)
                        .map(group -> new DeviceRegistrationOptionsResponse.CollectionGroupOption(
                                task.getId(), task.getName(), group.getId(), group.getName(), group.getCronExpression())))
                .toList();
        return new DeviceRegistrationOptionsResponse.ProtocolOption(
                protocol.getProtocolType().getId(), code, protocol.getProtocolType().getName(),
                lora ? "LORA_IDENTITY" : "IP_PORT", pointCount,
                "snmp".equals(code) && snmpPoints.existsByModelProtocolIdAndRequiresInstanceTrue(protocol.getId()),
                groups);
    }

    @Transactional
    public DeviceRegistrationResponse register(DeviceRegistrationRequest request) {
        DeviceModel model = findModel(request.device().modelId());
        DeviceModelProtocol protocol = model.getProtocols().stream()
                .filter(item -> item.getProtocolType().getId().equals(request.protocolTypeId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("protocol not supported by device model"));
        String code = protocol.getProtocolType().getCode().toLowerCase();
        boolean lora = "mqtt".equals(code) && "LORA_SENSOR".equals(model.getDeviceType().getCode());
        validateSettings(request, code, lora);
        if ("snmp".equals(code)
                && snmpPoints.existsByModelProtocolIdAndRequiresInstanceTrue(protocol.getId())
                && request.snmpInstance() == null) {
            throw new IllegalArgumentException("snmpInstance is required for this model");
        }
        validateGroup(request.collectionGroup(), model.getId(), request.protocolTypeId(), code);

        Integer deviceId = deviceService.createDevice(request.device(), false).id();
        Integer endpointId;
        if (lora) {
            DeviceRegistrationRequest.LoraIdentity identity = request.loraIdentity();
            endpointId = loraEndpointService.create(new DeviceLoraEndpointRequest(
                    deviceId, identity.idType(), identity.externalId(), identity.enabled())).id();
        } else {
            DeviceProtocolEndpointResponse endpoint = endpointService.createEndpoint(deviceId, request.endpoint(), false);
            endpointId = endpoint.id();
            if (request.snmpInstance() != null) {
                snmpInstanceService.createSnmpInstance(deviceId, endpointId, request.snmpInstance(), false);
            }
            if (request.modbus() != null) {
                modbusService.createEndpointModbus(deviceId, endpointId, request.modbus(), false);
                if (request.modbusReadings() != null) {
                    for (DeviceRegistrationRequest.ModbusReading reading : request.modbusReadings()) {
                        modbusReadingService.createReading(deviceId, endpointId,
                                new DeviceModbusReadingCreateRequest(reading.pointId(), reading.unitId(),
                                        reading.address(), reading.targetDeviceId() == null ? deviceId : reading.targetDeviceId(),
                                        reading.pointName(), reading.enabled()), false);
                    }
                }
            }
            if (request.collectionGroup() != null) {
                taskService.addGroupDevice(request.collectionGroup().taskId(), request.collectionGroup().groupId(), deviceId);
            } else if ("snmp".equals(code) || "modbus".equals(code)) {
                Device device = deviceRepository.findById(deviceId).orElseThrow();
                scriptSyncService.assignDeviceAndRegenerate(device, code);
            }
        }
        return new DeviceRegistrationResponse(deviceService.getDevice(deviceId), endpointId,
                request.collectionGroup() == null ? null : request.collectionGroup().taskId(),
                request.collectionGroup() == null ? null : request.collectionGroup().groupId());
    }

    private void validateSettings(DeviceRegistrationRequest request, String code, boolean lora) {
        if (lora) {
            if (request.loraIdentity() == null || request.endpoint() != null || request.snmpInstance() != null
                    || request.modbus() != null || request.modbusReadings() != null
                    || request.collectionGroup() != null) {
                throw new IllegalArgumentException("LoRa MQTT registration requires loraIdentity only");
            }
            return;
        }
        if (request.endpoint() == null || !request.protocolTypeId().equals(request.endpoint().protocolTypeId())
                || request.loraIdentity() != null) {
            throw new IllegalArgumentException("endpoint must match selected protocol");
        }
        if (request.snmpInstance() != null && !"snmp".equals(code)) {
            throw new IllegalArgumentException("snmpInstance requires SNMP protocol");
        }
        if (request.modbus() != null && !"modbus".equals(code)) {
            throw new IllegalArgumentException("modbus settings require Modbus protocol");
        }
        if ("modbus".equals(code) && request.modbus() == null) {
            throw new IllegalArgumentException("modbus settings are required");
        }
        if (!"modbus".equals(code) && request.modbusReadings() != null) {
            throw new IllegalArgumentException("modbusReadings require Modbus protocol");
        }
    }

    private void validateGroup(DeviceRegistrationRequest.CollectionGroup selection,
                               Integer modelId, Integer protocolTypeId, String code) {
        if (selection == null) return;
        if (!"snmp".equals(code) && !"modbus".equals(code)) {
            throw new IllegalArgumentException("collection group requires SNMP or Modbus protocol");
        }
        CollectionTask task = taskRepository.findById(selection.taskId())
                .orElseThrow(() -> new EntityNotFoundException("CollectionTask not found: " + selection.taskId()));
        if (!task.getDeviceModel().getId().equals(modelId)
                || !task.getScriptType().getId().equals(protocolTypeId)) {
            throw new IllegalArgumentException("collection group does not match model and protocol");
        }
        if (!task.isActive()) {
            throw new IllegalArgumentException("collection task is inactive");
        }
        boolean exists = task.getGroups().stream().anyMatch(group -> group.getId().equals(selection.groupId()) && group.isActive());
        if (!exists) throw new EntityNotFoundException("CollectionTaskGroup not found: " + selection.groupId());
    }

    private DeviceModel findModel(Integer modelId) {
        return modelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModel not found: " + modelId));
    }
}
