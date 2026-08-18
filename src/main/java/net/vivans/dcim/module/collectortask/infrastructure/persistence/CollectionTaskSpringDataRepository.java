package net.vivans.dcim.module.collectortask.infrastructure.persistence;

import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollectionTaskSpringDataRepository extends JpaRepository<CollectionTask, String> {

    @Override
    @EntityGraph(attributePaths = {"scriptType", "scriptType.codeGroup"})
    Optional<CollectionTask> findById(String id);

    @EntityGraph(attributePaths = {"scriptType", "scriptType.codeGroup"})
    @Query("SELECT ct FROM CollectionTask ct " +
            "WHERE (:name IS NULL OR LOWER(ct.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:active IS NULL OR ct.active = :active) " +
            "AND (:scriptTypeId IS NULL OR ct.scriptType.id = :scriptTypeId) " +
            "ORDER BY ct.createdDt DESC")
    List<CollectionTask> findAll(
            @Param("name") String name,
            @Param("active") Boolean active,
            @Param("scriptTypeId") Integer scriptTypeId
    );
}
