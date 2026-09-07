package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionScriptSyncService;
import net.vivans.dcim.module.device.api.dto.DeviceEndpointModbusCreateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceEndpointModbusResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceEndpointModbus;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.repository.DeviceEndpointModbusRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceEndpointModbusQueryService {

    private static final String MODBUS_PROTOCOL_CODE = "modbus";
    private static final String ENDPOINT_MUST_BE_MODBUS_MESSAGE = "endpoint protocol must be modbus";
    private static final String ALREADY_EXISTS_MESSAGE = "modbus endpoint already exists for this endpoint";

    private final DeviceRepository deviceRepository;
    private final DeviceProtocolEndpointRepository deviceProtocolEndpointRepository;
    private final DeviceEndpointModbusRepository deviceEndpointModbusRepository;
    private final CollectionScriptSyncService collectionScriptSyncService;

    public DeviceEndpointModbusResponse getEndpointModbus(Integer deviceId, Integer endpointId) {
        findDevice(deviceId);
        findEndpoint(endpointId, deviceId);
        return DeviceEndpointModbusResponse.from(findEndpointModbus(endpointId));
    }

    @Transactional
    public DeviceEndpointModbusResponse createEndpointModbus(
            Integer deviceId,
            Integer endpointId,
            DeviceEndpointModbusCreateRequest request
    ) {
        findDevice(deviceId);
        DeviceProtocolEndpoint endpoint = findEndpoint(endpointId, deviceId);
        validateModbusEndpoint(endpoint);

        if (deviceEndpointModbusRepository.existsByEndpointId(endpointId)) {
            throw new ConflictException(ALREADY_EXISTS_MESSAGE);
        }

        DeviceEndpointModbus endpointModbus = DeviceEndpointModbus.create(endpoint, request.unitId());
        DeviceEndpointModbus saved = deviceEndpointModbusRepository.save(endpointModbus);

        collectionScriptSyncService.regenerateByModelId(endpoint.getDevice().getDeviceModel().getId());

        return DeviceEndpointModbusResponse.from(saved);
    }

    @Transactional
    public DeviceEndpointModbusResponse updateEndpointModbus(
            Integer deviceId,
            Integer endpointId,
            DeviceEndpointModbusCreateRequest request
    ) {
        findDevice(deviceId);
        DeviceProtocolEndpoint endpoint = findEndpoint(endpointId, deviceId);
        validateModbusEndpoint(endpoint);

        DeviceEndpointModbus endpointModbus = findEndpointModbus(endpointId);
        endpointModbus.update(request.unitId());
        DeviceEndpointModbus saved = deviceEndpointModbusRepository.save(endpointModbus);

        collectionScriptSyncService.regenerateByModelId(endpoint.getDevice().getDeviceModel().getId());
        return DeviceEndpointModbusResponse.from(saved);
    }

    @Transactional
    public Integer deleteEndpointModbus(Integer deviceId, Integer endpointId) {
        findDevice(deviceId);
        DeviceProtocolEndpoint endpoint = findEndpoint(endpointId, deviceId);
        DeviceEndpointModbus endpointModbus = findEndpointModbus(endpointId);
        deviceEndpointModbusRepository.delete(endpointModbus);

        collectionScriptSyncService.regenerateByModelId(endpoint.getDevice().getDeviceModel().getId());
        return endpointId;
    }

    private DeviceEndpointModbus findEndpointModbus(Integer endpointId) {
        return deviceEndpointModbusRepository.findByEndpointId(endpointId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "DeviceEndpointModbus not found for endpoint: " + endpointId
                ));
    }

    private Device findDevice(Integer deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found: " + deviceId));
    }

    private DeviceProtocolEndpoint findEndpoint(Integer endpointId, Integer deviceId) {
        return deviceProtocolEndpointRepository.findByIdAndDeviceId(endpointId, deviceId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "DeviceProtocolEndpoint not found: " + endpointId
                ));
    }

    private void validateModbusEndpoint(DeviceProtocolEndpoint endpoint) {
        if (!MODBUS_PROTOCOL_CODE.equals(endpoint.getProtocolType().getCode())) {
            throw new IllegalArgumentException(ENDPOINT_MUST_BE_MODBUS_MESSAGE);
        }
    }

}
