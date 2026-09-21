package net.vivans.dcim.module.lora.infrastructure.persistence;

import net.vivans.dcim.module.lora.domain.model.LoraMqttSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoraMqttSourceSpringDataRepository extends JpaRepository<LoraMqttSource, Integer> {
    List<LoraMqttSource> findAllByOrderByIdAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
