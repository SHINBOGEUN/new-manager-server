package net.vivans.dcim.module.mqtt.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceMqttIdentityJpaRepository implements DeviceMqttIdentityRepository {

    private final DeviceMqttIdentitySpringDataRepository springDataRepository;

    @Override
    public DeviceMqttIdentity save(DeviceMqttIdentity endpoint) {
        return springDataRepository.save(endpoint);
    }

    @Override
    public Optional<DeviceMqttIdentity> findById(Integer id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<DeviceMqttIdentity> findAllByDeviceId(Integer deviceId) {
        return springDataRepository.findAllByDeviceId(deviceId);
    }

    @Override
    public List<DeviceMqttIdentity> findAllOrderByIdAsc() {
        return springDataRepository.findAllByOrderByIdAsc();
    }

    @Override
    public Optional<DeviceMqttIdentity> findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(
            MqttIdentityType idType, String normalizedExternalId) {
        return springDataRepository.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(idType, normalizedExternalId);
    }

    @Override
    public boolean existsByDeviceIdAndIdentityType(Integer deviceId, MqttIdentityType idType) {
        return springDataRepository.existsByDeviceIdAndIdentityType(deviceId, idType);
    }

    @Override
    public boolean existsByDeviceIdAndIdentityTypeAndIdNot(Integer deviceId, MqttIdentityType idType, Integer id) {
        return springDataRepository.existsByDeviceIdAndIdentityTypeAndIdNot(deviceId, idType, id);
    }

    @Override
    public void delete(DeviceMqttIdentity endpoint) {
        springDataRepository.delete(endpoint);
    }
}
