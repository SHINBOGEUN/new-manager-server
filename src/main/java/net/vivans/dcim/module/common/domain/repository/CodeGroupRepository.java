package net.vivans.dcim.module.common.domain.repository;

import net.vivans.dcim.module.common.domain.model.CodeGroup;

import java.util.List;
import java.util.Optional;

public interface CodeGroupRepository {

    List<CodeGroup> findAll();

    CodeGroup save(CodeGroup codeGroup);

    void delete(CodeGroup codeGroup);

    Optional<CodeGroup> findById(Integer id);

    boolean existsByGroupKeyAndIdNot(String groupKey, Integer id);

    boolean existsByGroupNameAndIdNot(String groupName, Integer id);
}
