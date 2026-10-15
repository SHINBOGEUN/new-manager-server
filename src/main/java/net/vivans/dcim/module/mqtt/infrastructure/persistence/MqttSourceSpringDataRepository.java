package net.vivans.dcim.module.mqtt.infrastructure.persistence;

import net.vivans.dcim.module.mqtt.domain.model.MqttSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MqttSourceSpringDataRepository extends JpaRepository<MqttSource, Integer> {
    List<MqttSource> findAllByOrderByIdAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
