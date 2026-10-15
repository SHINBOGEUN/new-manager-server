package net.vivans.dcim.module.lora.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointRequest;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointResponse;
import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttExternalIdNormalizer;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceLoraEndpointService {

    private final DeviceMqttIdentityRepository mqttIdentityRepository;
    private final DeviceRepository deviceRepository;
    private final LoraModelTypeValidator loraModelTypeValidator;
    private final ApplicationEventPublisher eventPublisher;

    public List<DeviceLoraEndpointResponse> getAll() {
        List<DeviceLoraEndpointResponse> responses = new ArrayList<>();
        for (DeviceMqttIdentity endpoint : mqttIdentityRepository.findAllOrderByIdAsc()) {
            if (LoraMqttSelection.isLora(endpoint)) {
                responses.add(DeviceLoraEndpointResponse.from(endpoint));
            }
        }
        return responses;
    }

    public List<DeviceLoraEndpointResponse> getAllByDeviceId(Integer deviceId) {
        List<DeviceLoraEndpointResponse> responses = new ArrayList<>();
        for (DeviceMqttIdentity endpoint : mqttIdentityRepository.findAllByDeviceId(deviceId)) {
            if (LoraMqttSelection.isLora(endpoint)) {
                responses.add(DeviceLoraEndpointResponse.from(endpoint));
            }
        }
        return responses;
    }

    @Transactional
    public DeviceLoraEndpointResponse create(DeviceLoraEndpointRequest request) {
        Device device = findDevice(request.deviceId());
        loraModelTypeValidator.requireLoraSensor(device);
        validateUnique(request.deviceId(), request.idType(), request.externalId(), null);
        boolean enabled = request.enabled() == null || request.enabled();
        DeviceMqttIdentity saved = mqttIdentityRepository.save(
                DeviceMqttIdentity.create(device, request.idType(), request.externalId(), enabled));
        eventPublisher.publishEvent(new LoraConfigChangedEvent("endpoint-created:" + saved.getId()));
        return DeviceLoraEndpointResponse.from(saved);
    }

    @Transactional
    public DeviceLoraEndpointResponse update(Integer id, DeviceLoraEndpointRequest request) {
        DeviceMqttIdentity endpoint = findEndpoint(id);
        loraModelTypeValidator.requireLoraSensor(endpoint.getDevice());
        if (!Objects.equals(endpoint.getDevice().getId(), request.deviceId())
                || endpoint.getIdentityType() != request.idType()) {
            throw new IllegalArgumentException("deviceId and idType cannot be changed on an existing identity");
        }
        validateUnique(request.deviceId(), request.idType(), request.externalId(), id);
        boolean enabled = request.enabled() == null || request.enabled();
        endpoint.update(request.externalId(), enabled);
        DeviceMqttIdentity saved = mqttIdentityRepository.save(endpoint);
        eventPublisher.publishEvent(new LoraConfigChangedEvent("endpoint-updated:" + id));
        return DeviceLoraEndpointResponse.from(saved);
    }

    @Transactional
    public Integer delete(Integer id) {
        DeviceMqttIdentity endpoint = findEndpoint(id);
        mqttIdentityRepository.delete(endpoint);
        eventPublisher.publishEvent(new LoraConfigChangedEvent("endpoint-deleted:" + id));
        return id;
    }

    private void validateUnique(Integer deviceId, MqttIdentityType idType,
                                 String externalId, Integer excludeId) {
        if (idType == null || !idType.isLora()) {
            throw new IllegalArgumentException("LoRa idType must be DEV_EUI or DEVICE_NAME");
        }
        boolean deviceTypeConflict = excludeId == null
                ? mqttIdentityRepository.existsByDeviceIdAndIdentityType(deviceId, idType)
                : mqttIdentityRepository.existsByDeviceIdAndIdentityTypeAndIdNot(deviceId, idType, excludeId);
        if (deviceTypeConflict) {
            throw new IllegalArgumentException("device already has an endpoint of this idType");
        }
        String normalized = MqttExternalIdNormalizer.normalize(idType, externalId);
        mqttIdentityRepository.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(idType, normalized)
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("externalId already registered to another device");
                });
    }

    private Device findDevice(Integer deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found: " + deviceId));
    }

    private DeviceMqttIdentity findEndpoint(Integer id) {
        return mqttIdentityRepository.findById(id)
                .filter(LoraMqttSelection::isLora)
                .orElseThrow(() -> new EntityNotFoundException("DeviceMqttIdentity not found: " + id));
    }
}
