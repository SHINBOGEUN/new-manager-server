package net.vivans.dcim.module.collectortask.domain.repository;

import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;

import java.util.List;
import java.util.Optional;

public interface CollectionTaskRepository {

    CollectionTask save(CollectionTask collectionTask);

    Optional<CollectionTask> findById(Integer id);

    List<CollectionTask> findAll(Integer modelId, Integer scriptTypeId, Boolean active);

    Optional<CollectionTask> findByModelIdAndScriptTypeId(Integer modelId, Integer scriptTypeId);

    List<CollectionTask> findAllByModelId(Integer modelId);

    boolean existsByModelIdAndScriptTypeId(Integer modelId, Integer scriptTypeId);

    boolean existsByModelId(Integer modelId);

    void delete(CollectionTask collectionTask);
}
