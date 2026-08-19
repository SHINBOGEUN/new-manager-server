package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionScriptSyncService;
import net.vivans.dcim.module.device.api.dto.DeviceSnmpInstanceCreateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceSnmpInstanceResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.model.DeviceSnmpInstance;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceSnmpInstanceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceSnmpInstanceQueryService {

    private static final String SNMP_PROTOCOL_CODE = "snmp";
    private static final String ENDPOINT_MUST_BE_SNMP_MESSAGE = "endpoint protocol must be snmp";
    private static final String NO_REQUIRES_INSTANCE_POINT_MESSAGE = "device model has no snmp point requiring instance";
    private static final String ALREADY_EXISTS_MESSAGE = "snmp instance already exists for this endpoint";

    private final DeviceRepository deviceRepository;
    private final DeviceProtocolEndpointRepository deviceProtocolEndpointRepository;
    private final DeviceSnmpInstanceRepository deviceSnmpInstanceRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;
    private final CollectionScriptSyncService collectionScriptSyncService;

    public DeviceSnmpInstanceResponse getSnmpInstance(Integer deviceId, Integer endpointId) {
        findDevice(deviceId);
        findEndpoint(endpointId, deviceId);
        return DeviceSnmpInstanceResponse.from(findSnmpInstance(endpointId));
    }

    @Transactional
    public DeviceSnmpInstanceResponse createSnmpInstance(
            Integer deviceId,
            Integer endpointId,
            DeviceSnmpInstanceCreateRequest request
    ) {
        findDevice(deviceId);
        DeviceProtocolEndpoint endpoint = findEndpoint(endpointId, deviceId);
        validateSnmpEndpoint(endpoint);
        validateModelHasRequiresInstancePoint(endpoint.getDevice());

        if (deviceSnmpInstanceRepository.existsByEndpointId(endpointId)) {
            throw new ConflictException(ALREADY_EXISTS_MESSAGE);
        }

        DeviceSnmpInstance snmpInstance = DeviceSnmpInstance.create(endpoint, request.instanceId());
        DeviceSnmpInstance saved = deviceSnmpInstanceRepository.save(snmpInstance);
        collectionScriptSyncService.regenerateByModelId(endpoint.getDevice().getDeviceModel().getId());
        return DeviceSnmpInstanceResponse.from(saved);
    }

    @Transactional
    public DeviceSnmpInstanceResponse updateSnmpInstance(
            Integer deviceId,
            Integer endpointId,
            DeviceSnmpInstanceCreateRequest request
    ) {
        findDevice(deviceId);
        DeviceProtocolEndpoint endpoint = findEndpoint(endpointId, deviceId);
        validateSnmpEndpoint(endpoint);
        validateModelHasRequiresInstancePoint(endpoint.getDevice());

        DeviceSnmpInstance snmpInstance = findSnmpInstance(endpointId);
        snmpInstance.update(request.instanceId());
        DeviceSnmpInstance saved = deviceSnmpInstanceRepository.save(snmpInstance);
        collectionScriptSyncService.regenerateByModelId(endpoint.getDevice().getDeviceModel().getId());
        return DeviceSnmpInstanceResponse.from(saved);
    }

    @Transactional
    public Integer deleteSnmpInstance(Integer deviceId, Integer endpointId) {
        findDevice(deviceId);
        DeviceProtocolEndpoint endpoint = findEndpoint(endpointId, deviceId);
        DeviceSnmpInstance snmpInstance = findSnmpInstance(endpointId);
        deviceSnmpInstanceRepository.delete(snmpInstance);
        collectionScriptSyncService.regenerateByModelId(endpoint.getDevice().getDeviceModel().getId());
        return endpointId;
    }

    private DeviceSnmpInstance findSnmpInstance(Integer endpointId) {
        return deviceSnmpInstanceRepository.findByEndpointId(endpointId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "DeviceSnmpInstance not found for endpoint: " + endpointId));
    }

    private Device findDevice(Integer deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found: " + deviceId));
    }

    private DeviceProtocolEndpoint findEndpoint(Integer endpointId, Integer deviceId) {
        return deviceProtocolEndpointRepository.findByIdAndDeviceId(endpointId, deviceId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "DeviceProtocolEndpoint not found: " + endpointId));
    }

    private void validateSnmpEndpoint(DeviceProtocolEndpoint endpoint) {
        if (!SNMP_PROTOCOL_CODE.equals(endpoint.getProtocolType().getCode())) {
            throw new IllegalArgumentException(ENDPOINT_MUST_BE_SNMP_MESSAGE);
        }
    }

    private void validateModelHasRequiresInstancePoint(Device device) {
        DeviceModel deviceModel = deviceModelRepository.findById(device.getDeviceModel().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "DeviceModel not found: " + device.getDeviceModel().getId()));

        DeviceModelProtocol snmpProtocol = null;
        for (DeviceModelProtocol protocol : deviceModel.getProtocols()) {
            if (SNMP_PROTOCOL_CODE.equals(protocol.getProtocolType().getCode())) {
                snmpProtocol = protocol;
                break;
            }
        }
        if (snmpProtocol == null) {
            throw new IllegalArgumentException(NO_REQUIRES_INSTANCE_POINT_MESSAGE);
        }

        if (!deviceModelSnmpPointRepository.existsByModelProtocolIdAndRequiresInstanceTrue(
                snmpProtocol.getId())) {
            throw new IllegalArgumentException(NO_REQUIRES_INSTANCE_POINT_MESSAGE);
        }
    }
}
