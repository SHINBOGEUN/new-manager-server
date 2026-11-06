package net.vivans.dcim.module.mqtt.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import net.vivans.dcim.module.mqtt.api.dto.MqttIdentityLookupResponse;
import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sensor Data가 신뢰할 수 있는 에이전트로 판별한 UUID만 등록한다. */
@Service
@RequiredArgsConstructor
public class GpuAgentRegistrationService {
    private final DeviceMqttIdentityRepository identities;
    private final DeviceRepository devices;
    private final DeviceModelRepository models;
    private final LocationNodeRepository locations;

    @Transactional
    public synchronized MqttIdentityLookupResponse register(String externalId, String hostname) {
        String id = UUID.fromString(externalId).toString();
        var existing = identities.findByIdentityTypeAndNormalizedExternalId(MqttIdentityType.UUID, id);
        if (existing.isPresent()) {
            var identity = existing.get();
            if (!identity.isEnabled() || !identity.getDevice().isEnabled()) {
                throw new IllegalStateException("Disabled identity cannot be automatically re-enabled");
            }
            return MqttIdentityLookupResponse.from(identity);
        }
        var model = models.findAll("SPATIVIA GPU Agent", "SPATIVIA").stream()
                .filter(m -> "SPATIVIA GPU Agent".equals(m.getName()) && "SPATIVIA".equals(m.getManufacturer()))
                .findFirst().orElseThrow(() -> new IllegalStateException("GPU agent catalog migration is required"));
        var location = locations.findByCode(Device.UNASSIGNED_LOCATION_CODE)
                .orElseThrow(() -> new IllegalStateException("UNASSIGNED location is required"));
        String name = hostname == null ? "GPU server" : hostname.strip();
        if (name.isEmpty()) name = "GPU server";
        name = name.substring(0, Math.min(name.length(), 200));
        // 같은 호스트명이거나 UUID 앞부분이 같아도 서로 다른 장비로 등록한다.
        name += " (" + id + ")";
        var device = devices.save(Device.create(model, location, name, "GPU agent automatic registration"));
        return MqttIdentityLookupResponse.from(identities.save(
                DeviceMqttIdentity.create(device, MqttIdentityType.UUID, id, true)));
    }
}

