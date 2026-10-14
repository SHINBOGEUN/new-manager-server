package net.vivans.dcim.module.mqtt.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.mqtt.api.dto.DeviceMqttIdentityRequest;
import net.vivans.dcim.module.mqtt.api.dto.DeviceMqttIdentityResponse;
import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttExternalIdNormalizer;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceMqttIdentityService {
    private final DeviceMqttIdentityRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public List<DeviceMqttIdentityResponse> getByDeviceId(Integer deviceId) {
        return repository.findAllByDeviceId(deviceId).stream()
                .sorted(Comparator.comparing(DeviceMqttIdentity::getId))
                .map(DeviceMqttIdentityResponse::from).toList();
    }

    public Map<Integer, List<DeviceMqttIdentityResponse>> getByDeviceIds(Collection<Integer> deviceIds) {
        if (deviceIds.isEmpty()) return Map.of();
        Map<Integer, List<DeviceMqttIdentityResponse>> result = new HashMap<>();
        for (DeviceMqttIdentity identity : repository.findAllByDeviceIdIn(deviceIds)) {
            result.computeIfAbsent(identity.getDevice().getId(), ignored -> new ArrayList<>())
                    .add(DeviceMqttIdentityResponse.from(identity));
        }
        result.values().forEach(list -> list.sort(Comparator.comparing(DeviceMqttIdentityResponse::id)));
        return result;
    }

    /** null은 기존 값 유지, 빈 목록은 연결 제거. 동일 유형의 값 변경은 기존 PK와 device_id를 유지한다. */
    @Transactional
    public void replace(Device device, List<DeviceMqttIdentityRequest> requests) {
        List<DeviceMqttIdentity> existing = repository.findAllByDeviceId(device.getId());
        if (requests == null && existing.isEmpty()) return;
        boolean hasIdentities = requests == null ? !existing.isEmpty() : !requests.isEmpty();
        if (hasIdentities && device.getDeviceModel().getProtocols().stream()
                .noneMatch(protocol -> "mqtt".equalsIgnoreCase(protocol.getProtocolType().getCode()))) {
            throw new IllegalArgumentException("MQTT identities require a model supporting MQTT");
        }
        boolean loraModel = "LORA_SENSOR".equals(device.getDeviceModel().getDeviceType().getCode());
        if (requests == null) {
            existing.forEach(identity -> validateType(identity.getIdentityType(), loraModel));
            return;
        }
        Map<MqttIdentityType, DeviceMqttIdentity> byType = new EnumMap<>(MqttIdentityType.class);
        existing.forEach(identity -> byType.put(identity.getIdentityType(), identity));
        Set<MqttIdentityType> requestedTypes = EnumSet.noneOf(MqttIdentityType.class);
        for (DeviceMqttIdentityRequest request : requests) {
            if (request == null || request.identityType() == null) {
                throw new IllegalArgumentException("identityType is required");
            }
            validateType(request.identityType(), loraModel);
            if (!requestedTypes.add(request.identityType())) {
                throw new IllegalArgumentException("only one identity per type is allowed for a device");
            }
            String normalized = MqttExternalIdNormalizer.normalize(request.identityType(), request.externalId());
            repository.findByIdentityTypeAndNormalizedExternalId(request.identityType(), normalized)
                    .filter(identity -> !identity.getDevice().getId().equals(device.getId()))
                    .ifPresent(identity -> { throw new ConflictException("externalId already registered to another device"); });
        }
        for (DeviceMqttIdentity identity : existing) {
            if (!requestedTypes.contains(identity.getIdentityType())) repository.delete(identity);
        }
        boolean changed = existing.size() != requests.size();
        for (DeviceMqttIdentityRequest request : requests) {
            DeviceMqttIdentity identity = byType.get(request.identityType());
            boolean enabled = request.enabled() != null ? request.enabled() : identity == null || identity.isEnabled();
            if (identity == null) {
                repository.save(DeviceMqttIdentity.create(device, request.identityType(), request.externalId(), enabled));
                changed = true;
            } else if (!identity.getExternalId().equals(request.externalId().trim()) || identity.isEnabled() != enabled) {
                identity.update(request.externalId(), enabled);
                repository.save(identity);
                changed = true;
            }
        }
        if (changed) eventPublisher.publishEvent(new MqttConfigChangedEvent("identities-updated:" + device.getId()));
    }

    @Transactional
    public void deleteByDeviceId(Integer deviceId) {
        List<DeviceMqttIdentity> identities = repository.findAllByDeviceId(deviceId);
        identities.forEach(repository::delete);
        if (!identities.isEmpty()) eventPublisher.publishEvent(new MqttConfigChangedEvent("device-deleted:" + deviceId));
    }

    private static void validateType(MqttIdentityType type, boolean loraModel) {
        if (type.isLora() != loraModel) {
            throw new IllegalArgumentException(loraModel
                    ? "LoRa identity must be DEV_EUI or DEVICE_NAME"
                    : "MQTT identity must be MAC, UUID or SERIAL for this model");
        }
    }
}
