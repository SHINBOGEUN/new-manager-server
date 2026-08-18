package net.vivans.dcim.module.collectortask.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CollectionTaskJpaRepository implements CollectionTaskRepository {

    private final CollectionTaskSpringDataRepository springDataRepository;

    @Override
    public CollectionTask save(CollectionTask collectionTask) {
        return springDataRepository.save(collectionTask);
    }

    @Override
    public Optional<CollectionTask> findById(String id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<CollectionTask> findAll(String name, Boolean active, Integer scriptTypeId) {
        return springDataRepository.findAll(blankToNull(name), active, scriptTypeId);
    }

    @Override
    public void delete(CollectionTask collectionTask) {
        springDataRepository.delete(collectionTask);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
