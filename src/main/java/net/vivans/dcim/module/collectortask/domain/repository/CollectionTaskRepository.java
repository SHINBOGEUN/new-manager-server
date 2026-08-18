package net.vivans.dcim.module.collectortask.domain.repository;

import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;

import java.util.List;
import java.util.Optional;

public interface CollectionTaskRepository {

    CollectionTask save(CollectionTask collectionTask);

    Optional<CollectionTask> findById(String id);

    List<CollectionTask> findAll(String name, Boolean active, Integer scriptTypeId);

    void delete(CollectionTask collectionTask);
}
