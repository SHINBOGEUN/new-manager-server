package net.vivans.dcim.module.mqtt.infrastructure.persistence;

import net.vivans.dcim.module.mqtt.domain.model.MqttSourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MqttSourceStatusSpringDataRepository extends JpaRepository<MqttSourceStatus, Integer> {
    List<MqttSourceStatus> findAllBySourceIdIn(Collection<Integer> sourceIds);
}
