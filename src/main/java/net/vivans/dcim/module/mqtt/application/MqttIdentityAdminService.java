package net.vivans.dcim.module.mqtt.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.mqtt.api.dto.MqttIdentityAdminRequest;
import net.vivans.dcim.module.mqtt.api.dto.MqttIdentityAdminResponse;
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
public class MqttIdentityAdminService {

    private final DeviceMqttIdentityRepository mqttIdentityRepository;
    private final DeviceRepository deviceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<MqttIdentityAdminResponse> getAll() {
        List<MqttIdentityAdminResponse> responses = new ArrayList<>();
        for (DeviceMqttIdentity endpoint : mqttIdentityRepository.findAllOrderByIdAsc()) {
            responses.add(MqttIdentityAdminResponse.from(endpoint));
        }
        return responses;
    }

    public List<MqttIdentityAdminResponse> getAllByDeviceId(Integer deviceId) {
        List<MqttIdentityAdminResponse> responses = new ArrayList<>();
        for (DeviceMqttIdentity endpoint : mqttIdentityRepository.findAllByDeviceId(deviceId)) {
            responses.add(MqttIdentityAdminResponse.from(endpoint));
        }
        return responses;
    }

    @Transactional
    public MqttIdentityAdminResponse create(MqttIdentityAdminRequest request) {
        Device device = findDevice(request.deviceId());
        validateDevice(device, request.idType());
        validateUnique(request.deviceId(), request.idType(), request.externalId(), null);
        boolean enabled = request.enabled() == null || request.enabled();
        DeviceMqttIdentity saved = mqttIdentityRepository.save(
                DeviceMqttIdentity.create(device, request.idType(), request.externalId(), enabled));
        eventPublisher.publishEvent(new MqttConfigChangedEvent("endpoint-created:" + saved.getId()));
        return MqttIdentityAdminResponse.from(saved);
    }

    @Transactional
    public MqttIdentityAdminResponse update(Integer id, MqttIdentityAdminRequest request) {
        DeviceMqttIdentity endpoint = findEndpoint(id);
        validateDevice(endpoint.getDevice(), request.idType());
        if (!Objects.equals(endpoint.getDevice().getId(), request.deviceId())
                || endpoint.getIdentityType() != request.idType()) {
            throw new IllegalArgumentException("deviceId and idType cannot be changed on an existing identity");
        }
        validateUnique(request.deviceId(), request.idType(), request.externalId(), id);
        boolean enabled = request.enabled() == null || request.enabled();
        endpoint.update(request.externalId(), enabled);
        DeviceMqttIdentity saved = mqttIdentityRepository.save(endpoint);
        eventPublisher.publishEvent(new MqttConfigChangedEvent("endpoint-updated:" + id));
        return MqttIdentityAdminResponse.from(saved);
    }

    @Transactional
    public Integer delete(Integer id) {
        DeviceMqttIdentity endpoint = findEndpoint(id);
        mqttIdentityRepository.delete(endpoint);
        eventPublisher.publishEvent(new MqttConfigChangedEvent("endpoint-deleted:" + id));
        return id;
    }

    private void validateUnique(Integer deviceId, MqttIdentityType idType,
                                 String externalId, Integer excludeId) {
        if (idType == null) throw new IllegalArgumentException("idType is required");
        boolean deviceTypeConflict = excludeId == null
                ? mqttIdentityRepository.existsByDeviceIdAndIdentityType(deviceId, idType)
                : mqttIdentityRepository.existsByDeviceIdAndIdentityTypeAndIdNot(deviceId, idType, excludeId);
        if (deviceTypeConflict) {
            throw new IllegalArgumentException("device already has an endpoint of this idType");
        }
        String normalized = MqttExternalIdNormalizer.normalize(idType, externalId);
        mqttIdentityRepository.findByIdentityTypeAndNormalizedExternalId(idType, normalized)
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("externalId already registered to another device");
                });
    }

    private void validateDevice(Device device, MqttIdentityType type) {
        if (type == null) throw new IllegalArgumentException("idType is required");
        if (device.getDeviceModel().getProtocols().stream().noneMatch(p -> "mqtt".equalsIgnoreCase(p.getProtocolType().getCode()))) {
            throw new IllegalArgumentException("MQTT 지원 모델의 장비를 선택하세요.");
        }
        boolean lora = "LORA_SENSOR".equals(device.getDeviceModel().getDeviceType().getCode());
        if (type.isLora() != lora) throw new IllegalArgumentException(lora
                ? "LoRa 장비는 DEV_EUI 또는 DEVICE_NAME을 사용하세요."
                : "에이전트 장비는 MAC, UUID 또는 SERIAL을 사용하세요.");
    }

    private Device findDevice(Integer deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found: " + deviceId));
    }

    private DeviceMqttIdentity findEndpoint(Integer id) {
        return mqttIdentityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DeviceMqttIdentity not found: " + id));
    }
}
