package net.vivans.dcim.module.lora.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.domain.model.LoraMqttSource;
import net.vivans.dcim.module.lora.domain.repository.LoraMqttSourceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LoraMqttSourceJpaRepository implements LoraMqttSourceRepository {
    private final LoraMqttSourceSpringDataRepository springDataRepository;
    public LoraMqttSource save(LoraMqttSource source) { return springDataRepository.save(source); }
    public Optional<LoraMqttSource> findById(Integer id) { return springDataRepository.findById(id); }
    public List<LoraMqttSource> findAllOrderByIdAsc() { return springDataRepository.findAllByOrderByIdAsc(); }
    public boolean existsByNameIgnoreCase(String name) { return springDataRepository.existsByNameIgnoreCase(name); }
    public boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id) { return springDataRepository.existsByNameIgnoreCaseAndIdNot(name, id); }
    public void delete(LoraMqttSource source) { springDataRepository.delete(source); }
}
