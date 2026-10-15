package net.vivans.dcim.module.mqtt.domain.repository;

import net.vivans.dcim.module.mqtt.domain.model.MqttSourceStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MqttSourceStatusRepository {
    MqttSourceStatus save(MqttSourceStatus status);
    void delete(MqttSourceStatus status);
    Optional<MqttSourceStatus> findBySourceId(Integer sourceId);
    List<MqttSourceStatus> findAllBySourceIdIn(Collection<Integer> sourceIds);
}
