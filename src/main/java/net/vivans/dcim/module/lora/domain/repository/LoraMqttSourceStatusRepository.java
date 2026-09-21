package net.vivans.dcim.module.lora.domain.repository;

import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoraMqttSourceStatusRepository {
    LoraMqttSourceStatus save(LoraMqttSourceStatus status);
    Optional<LoraMqttSourceStatus> findBySourceId(Integer sourceId);
    List<LoraMqttSourceStatus> findAllBySourceIdIn(Collection<Integer> sourceIds);
}
