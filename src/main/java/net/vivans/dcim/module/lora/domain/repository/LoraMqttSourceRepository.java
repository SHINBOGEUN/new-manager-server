package net.vivans.dcim.module.lora.domain.repository;

import net.vivans.dcim.module.lora.domain.model.LoraMqttSource;

import java.util.List;
import java.util.Optional;

public interface LoraMqttSourceRepository {
    LoraMqttSource save(LoraMqttSource source);
    Optional<LoraMqttSource> findById(Integer id);
    List<LoraMqttSource> findAllOrderByIdAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
    void delete(LoraMqttSource source);
}
