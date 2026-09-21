package net.vivans.dcim.module.lora.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceStatus;
import net.vivans.dcim.module.lora.domain.repository.LoraMqttSourceStatusRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LoraMqttSourceStatusJpaRepository implements LoraMqttSourceStatusRepository {
    private final LoraMqttSourceStatusSpringDataRepository springDataRepository;
    public LoraMqttSourceStatus save(LoraMqttSourceStatus status) { return springDataRepository.save(status); }
    public Optional<LoraMqttSourceStatus> findBySourceId(Integer sourceId) { return springDataRepository.findById(sourceId); }
    public List<LoraMqttSourceStatus> findAllBySourceIdIn(Collection<Integer> sourceIds) { return springDataRepository.findAllBySourceIdIn(sourceIds); }
}
