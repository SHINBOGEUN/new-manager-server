package net.vivans.dcim.module.mqtt.domain.repository;

import net.vivans.dcim.module.mqtt.domain.model.MqttSource;

import java.util.List;
import java.util.Optional;

public interface MqttSourceRepository {
    MqttSource save(MqttSource source);
    Optional<MqttSource> findById(Integer id);
    List<MqttSource> findAllOrderByIdAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
    void delete(MqttSource source);
}
