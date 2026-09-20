package net.vivans.dcim.module.lora.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointRequest;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointResponse;
import net.vivans.dcim.module.lora.domain.model.DeviceLoraEndpoint;
import net.vivans.dcim.module.lora.domain.model.LoraExternalIdNormalizer;
import net.vivans.dcim.module.lora.domain.repository.DeviceLoraEndpointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceLoraEndpointService {

    private final DeviceLoraEndpointRepository deviceLoraEndpointRepository;
    private final DeviceRepository deviceRepository;
    private final LoraModelTypeValidator loraModelTypeValidator;

    public List<DeviceLoraEndpointResponse> getAll() {
        List<DeviceLoraEndpointResponse> responses = new ArrayList<>();
        for (DeviceLoraEndpoint endpoint : deviceLoraEndpointRepository.findAllOrderByIdAsc()) {
            responses.add(DeviceLoraEndpointResponse.from(endpoint));
        }
        return responses;
    }

    public List<DeviceLoraEndpointResponse> getAllByDeviceId(Integer deviceId) {
        List<DeviceLoraEndpointResponse> responses = new ArrayList<>();
        for (DeviceLoraEndpoint endpoint : deviceLoraEndpointRepository.findAllByDeviceId(deviceId)) {
            responses.add(DeviceLoraEndpointResponse.from(endpoint));
        }
        return responses;
    }

    @Transactional
    public DeviceLoraEndpointResponse create(DeviceLoraEndpointRequest request) {
        Device device = findDevice(request.deviceId());
        loraModelTypeValidator.requireLoraSensor(device);
        validateUnique(request.deviceId(), request.idType(), request.externalId(), null);
        boolean enabled = request.enabled() == null || request.enabled();
        DeviceLoraEndpoint saved = deviceLoraEndpointRepository.save(
                DeviceLoraEndpoint.create(device, request.idType(), request.externalId(), enabled));
        return DeviceLoraEndpointResponse.from(saved);
    }

    @Transactional
    public DeviceLoraEndpointResponse update(Integer id, DeviceLoraEndpointRequest request) {
        DeviceLoraEndpoint endpoint = findEndpoint(id);
        loraModelTypeValidator.requireLoraSensor(endpoint.getDevice());
        validateUnique(request.deviceId(), request.idType(), request.externalId(), id);
        boolean enabled = request.enabled() == null || request.enabled();
        endpoint.update(request.externalId(), enabled);
        return DeviceLoraEndpointResponse.from(deviceLoraEndpointRepository.save(endpoint));
    }

    @Transactional
    public Integer delete(Integer id) {
        DeviceLoraEndpoint endpoint = findEndpoint(id);
        deviceLoraEndpointRepository.delete(endpoint);
        return id;
    }

    private void validateUnique(Integer deviceId, net.vivans.dcim.module.lora.domain.model.LoraIdType idType,
                                 String externalId, Integer excludeId) {
        boolean deviceTypeConflict = excludeId == null
                ? deviceLoraEndpointRepository.existsByDeviceIdAndIdType(deviceId, idType)
                : deviceLoraEndpointRepository.existsByDeviceIdAndIdTypeAndIdNot(deviceId, idType, excludeId);
        if (deviceTypeConflict) {
            throw new IllegalArgumentException("device already has an endpoint of this idType");
        }
        String normalized = LoraExternalIdNormalizer.normalize(externalId);
        deviceLoraEndpointRepository.findByIdTypeAndNormalizedExternalIdAndEnabledTrue(idType, normalized)
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("externalId already registered to another device");
                });
    }

    private Device findDevice(Integer deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found: " + deviceId));
    }

    private DeviceLoraEndpoint findEndpoint(Integer id) {
        return deviceLoraEndpointRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DeviceLoraEndpoint not found: " + id));
    }
}
