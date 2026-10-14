package net.vivans.dcim.module.mqtt.domain.repository;

import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;

import java.util.List;
import java.util.Optional;

public interface DeviceMqttIdentityRepository {

    DeviceMqttIdentity save(DeviceMqttIdentity endpoint);

    Optional<DeviceMqttIdentity> findById(Integer id);

    List<DeviceMqttIdentity> findAllByDeviceId(Integer deviceId);

    List<DeviceMqttIdentity> findAllOrderByIdAsc();

    Optional<DeviceMqttIdentity> findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(
            MqttIdentityType idType, String normalizedExternalId);

    boolean existsByDeviceIdAndIdentityType(Integer deviceId, MqttIdentityType idType);

    boolean existsByDeviceIdAndIdentityTypeAndIdNot(Integer deviceId, MqttIdentityType idType, Integer id);

    void delete(DeviceMqttIdentity endpoint);
}
