package net.vivans.dcim.module.collectortask.infrastructure.persistence;

import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollectionTaskSpringDataRepository extends JpaRepository<CollectionTask, Integer> {

    @Override
    @EntityGraph(attributePaths = {
            "deviceModel",
            "scriptType",
            "scriptType.codeGroup",
            "groups"
    })
    Optional<CollectionTask> findById(Integer id);

    @EntityGraph(attributePaths = {
            "deviceModel",
            "scriptType",
            "scriptType.codeGroup",
            "groups"
    })
    @Query("SELECT ct FROM CollectionTask ct "
            + "WHERE (:modelId IS NULL OR ct.deviceModel.id = :modelId) "
            + "AND (:scriptTypeId IS NULL OR ct.scriptType.id = :scriptTypeId) "
            + "AND (:active IS NULL OR ct.active = :active) "
            + "ORDER BY ct.id ASC")
    List<CollectionTask> findAll(
            @Param("modelId") Integer modelId,
            @Param("scriptTypeId") Integer scriptTypeId,
            @Param("active") Boolean active
    );

    @EntityGraph(attributePaths = {
            "deviceModel",
            "scriptType",
            "scriptType.codeGroup",
            "groups"
    })
    Optional<CollectionTask> findByDeviceModelIdAndScriptTypeId(Integer modelId, Integer scriptTypeId);

    @EntityGraph(attributePaths = {
            "deviceModel",
            "scriptType",
            "scriptType.codeGroup",
            "groups"
    })
    List<CollectionTask> findAllByDeviceModelId(Integer modelId);

    boolean existsByDeviceModelIdAndScriptTypeId(Integer modelId, Integer scriptTypeId);

    boolean existsByDeviceModelId(Integer modelId);
}
