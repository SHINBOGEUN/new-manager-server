package net.vivans.dcim.module.mqtt.infrastructure.persistence;

import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceMqttIdentitySpringDataRepository extends JpaRepository<DeviceMqttIdentity, Integer> {

    @EntityGraph(attributePaths = {"device", "device.deviceModel"})
    List<DeviceMqttIdentity> findAllByDeviceId(Integer deviceId);

    @EntityGraph(attributePaths = {"device", "device.deviceModel"})
    List<DeviceMqttIdentity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"device", "device.deviceModel"})
    Optional<DeviceMqttIdentity> findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(
            MqttIdentityType idType, String normalizedExternalId);

    boolean existsByDeviceIdAndIdentityType(Integer deviceId, MqttIdentityType idType);

    boolean existsByDeviceIdAndIdentityTypeAndIdNot(Integer deviceId, MqttIdentityType idType, Integer id);
}
