package net.vivans.dcim.module.location.infrastructure.persistence;

import net.vivans.dcim.module.location.domain.model.LocationNode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface LocationNodeSpringDataRepository extends JpaRepository<LocationNode, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM LocationNode n WHERE n.code = :code")
    Optional<LocationNode> lockByCode(@Param("code") String code);

    boolean existsByCode(String code);

    boolean existsByParent_Code(String parentCode);

    boolean existsByParentIsNullAndName(String name);

    boolean existsByParentAndName(LocationNode parent, String name);

    boolean existsByParentIsNullAndNameAndCodeNot(String name, String code);

    boolean existsByParentAndNameAndCodeNot(LocationNode parent, String name, String code);

    List<LocationNode> findByParent_Code(String parentCode);

    @EntityGraph(attributePaths = {"parent", "locationType"})
    List<LocationNode> findAll();
}
