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
    public CollectionTask saveAndFlush(CollectionTask collectionTask) {
        return springDataRepository.saveAndFlush(collectionTask);
    }

    @Override
    public Optional<CollectionTask> findById(Integer id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<CollectionTask> findAll(Integer modelId, Integer scriptTypeId, Boolean active) {
        return springDataRepository.findAll(modelId, scriptTypeId, active);
    }

    @Override
    public Optional<CollectionTask> findByModelIdAndScriptTypeId(Integer modelId, Integer scriptTypeId) {
        return springDataRepository.findByDeviceModelIdAndScriptTypeId(modelId, scriptTypeId);
    }

    @Override
    public List<CollectionTask> findAllByModelId(Integer modelId) {
        return springDataRepository.findAllByDeviceModelId(modelId);
    }

    @Override
    public boolean existsByModelIdAndScriptTypeId(Integer modelId, Integer scriptTypeId) {
        return springDataRepository.existsByDeviceModelIdAndScriptTypeId(modelId, scriptTypeId);
    }

    @Override
    public boolean existsByModelId(Integer modelId) {
        return springDataRepository.existsByDeviceModelId(modelId);
    }

    @Override
    public void delete(CollectionTask collectionTask) {
        springDataRepository.delete(collectionTask);
    }
}
