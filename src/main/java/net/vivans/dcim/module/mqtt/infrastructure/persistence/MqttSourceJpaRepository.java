package net.vivans.dcim.module.mqtt.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.domain.model.MqttSource;
import net.vivans.dcim.module.mqtt.domain.repository.MqttSourceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MqttSourceJpaRepository implements MqttSourceRepository {
    private final MqttSourceSpringDataRepository springDataRepository;
    public MqttSource save(MqttSource source) { return springDataRepository.save(source); }
    public Optional<MqttSource> findById(Integer id) { return springDataRepository.findById(id); }
    public List<MqttSource> findAllOrderByIdAsc() { return springDataRepository.findAllByOrderByIdAsc(); }
    public boolean existsByNameIgnoreCase(String name) { return springDataRepository.existsByNameIgnoreCase(name); }
    public boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id) { return springDataRepository.existsByNameIgnoreCaseAndIdNot(name, id); }
    public void delete(MqttSource source) { springDataRepository.delete(source); }
}
