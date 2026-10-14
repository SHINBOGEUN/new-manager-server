package net.vivans.dcim.module.mqtt.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.domain.model.MqttSourceStatus;
import net.vivans.dcim.module.mqtt.domain.repository.MqttSourceStatusRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MqttSourceStatusJpaRepository implements MqttSourceStatusRepository {
    private final MqttSourceStatusSpringDataRepository springDataRepository;
    public MqttSourceStatus save(MqttSourceStatus status) { return springDataRepository.save(status); }
    public void delete(MqttSourceStatus status) { springDataRepository.delete(status); }
    public Optional<MqttSourceStatus> findBySourceId(Integer sourceId) { return springDataRepository.findById(sourceId); }
    public List<MqttSourceStatus> findAllBySourceIdIn(Collection<Integer> sourceIds) { return springDataRepository.findAllBySourceIdIn(sourceIds); }
}
