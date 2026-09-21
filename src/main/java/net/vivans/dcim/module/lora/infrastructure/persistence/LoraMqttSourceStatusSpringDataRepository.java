package net.vivans.dcim.module.lora.infrastructure.persistence;

import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LoraMqttSourceStatusSpringDataRepository extends JpaRepository<LoraMqttSourceStatus, Integer> {
    List<LoraMqttSourceStatus> findAllBySourceIdIn(Collection<Integer> sourceIds);
}
