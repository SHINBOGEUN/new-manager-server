package net.vivans.dcim.module.mqtt.application;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.api.dto.*;
import net.vivans.dcim.module.mqtt.domain.model.*;
import net.vivans.dcim.module.mqtt.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sensor Data가 사용하는 공통 MQTT 설정 조회. LoRa 전용 API는 호환용으로 유지한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MqttLookupService {
    private final DeviceMqttIdentityRepository identityRepository;
    private final DeviceModelMqttPointRepository pointRepository;

    public Optional<MqttIdentityLookupResponse> resolve(MqttIdentityType identityType, String externalId) {
        String normalized = MqttExternalIdNormalizer.normalize(identityType, externalId);
        return identityRepository.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(identityType, normalized)
                .filter(identity -> identity.getDevice().isEnabled())
                .map(MqttIdentityLookupResponse::from);
    }

    public List<MqttIdentityLookupResponse> getAllEnabledIdentities() {
        return identityRepository.findAllOrderByIdAsc().stream()
                .filter(identity -> identity.isEnabled() && identity.getDevice().isEnabled())
                .map(MqttIdentityLookupResponse::from).toList();
    }

    public List<DeviceModelMqttPointResponse> getAllEnabledModelMappings() {
        return pointRepository.findAllEnabled().stream().filter(DeviceModelMqttPoint::isEnabled)
                .map(DeviceModelMqttPointResponse::from).toList();
    }
}
