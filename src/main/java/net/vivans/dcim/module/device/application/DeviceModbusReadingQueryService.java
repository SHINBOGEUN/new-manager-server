package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceModbusReadingCreateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceModbusReadingResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.repository.DeviceEndpointModbusRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelModbusPointRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceModbusReadingQueryService {
    private static final String MODBUS_PROTOCOL_CODE = "modbus";

    private final DeviceRepository deviceRepository;
    private final DeviceProtocolEndpointRepository endpointRepository;
    private final DeviceEndpointModbusRepository endpointModbusRepository;
    private final DeviceModelModbusPointRepository pointRepository;
    private final DeviceModbusReadingRepository readingRepository;

    @Transactional
    public DeviceModbusReadingResponse createReading(
            Integer deviceId,
            Integer endpointId,
            DeviceModbusReadingCreateRequest request
    ) {
        findDevice(deviceId);

        DeviceProtocolEndpoint endpoint = findEndpoint(deviceId, endpointId);
        validateModbusEndpoint(endpoint);
        requireModbusConfig(endpointId);

        DeviceModelModbusPoint point = findSourceModelPoint(
                endpoint,
                request.pointId()
        );

        Device targetDevice = findDevice(request.targetDeviceId());

        if (readingRepository.existsByTargetDeviceIdAndPointName(
                targetDevice.getId(),
                request.pointName()
        )) {
            throw new ConflictException(
                    "Modbus reading already exists for target device and point name"
            );
        }

        boolean enabled = request.enabled() == null || request.enabled();

        DeviceModbusReading reading = DeviceModbusReading.create(
                endpoint,
                point,
                request.unitId(),
                request.address(),
                targetDevice,
                request.pointName(),
                enabled
        );

        DeviceModbusReading saved = readingRepository.save(reading);
        return DeviceModbusReadingResponse.from(saved);
    }

    private Device findDevice(Integer deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Device not found: " + deviceId
                ));
    }

    private DeviceProtocolEndpoint findEndpoint(
            Integer deviceId,
            Integer endpointId
    ) {
        return endpointRepository.findByIdAndDeviceId(endpointId, deviceId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "DeviceProtocolEndpoint not found: " + endpointId
                ));
    }

    private void validateModbusEndpoint(DeviceProtocolEndpoint endpoint) {
        if (!MODBUS_PROTOCOL_CODE.equals(
                endpoint.getProtocolType().getCode()
        )) {
            throw new IllegalArgumentException(
                    "endpoint protocol must be modbus"
            );
        }
    }

    private void requireModbusConfig(Integer endpointId) {
        if (!endpointModbusRepository.existsByEndpointId(endpointId)) {
            throw new EntityNotFoundException(
                    "DeviceEndpointModbus not found for endpoint: " + endpointId
            );
        }
    }

    private DeviceModelModbusPoint findSourceModelPoint(
            DeviceProtocolEndpoint endpoint,
            Integer pointId
    ) {
        for (DeviceModelProtocol protocol
                : endpoint.getDevice().getDeviceModel().getProtocols()) {

            if (endpoint.getProtocolType().getId()
                    .equals(protocol.getProtocolType().getId())) {

                return pointRepository.findByIdAndModelProtocolId(
                                pointId,
                                protocol.getId()
                        )
                        .orElseThrow(() -> new EntityNotFoundException(
                                "DeviceModelModbusPoint not found for source model: "
                                        + pointId
                        ));
            }
        }

        throw new IllegalArgumentException(
                "source device model does not support endpoint protocol"
        );
    }
}
