package net.vivans.dcim.module.common.infrastructure.persistence;

import net.vivans.dcim.module.common.domain.model.CommonCode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommonCodeSpringDataRepository extends JpaRepository<CommonCode, Integer> {

    @EntityGraph(attributePaths = {"codeGroup"})
    Optional<CommonCode> findByCodeGroup_GroupKeyAndCode(String groupKey, String code);

    boolean existsByCodeGroupIdAndCode(Integer groupId, String code);
    boolean existsByCodeAndIdNot(String code, Integer id);
    boolean existsByNameAndIdNot(String name, Integer id);
    List<CommonCode> findByCodeGroupId(Integer codeGroupId);
}
