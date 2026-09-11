package net.vivans.dcim.module.common.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.repository.CodeGroupRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CodeGroupJpaRepository implements CodeGroupRepository {

    private final CodeGroupSpringDataRepository springDataRepository;

    @Override
    public List<CodeGroup> findAll() {
        return springDataRepository.findAll();
    }

    @Override
    public Optional<CodeGroup> findById(Integer id) {
        return springDataRepository.findById(id);
    }

    @Override
    public boolean existsByGroupKeyAndIdNot(String groupKey, Integer id) {
        return springDataRepository.existsByGroupKeyAndIdNot(groupKey, id);
    }

    @Override
    public boolean existsByGroupNameAndIdNot(String groupName, Integer id) {
        return springDataRepository.existsByGroupNameAndIdNot(groupName, id);
    }

    @Override
    public CodeGroup save(CodeGroup codeGroup) {
        return springDataRepository.save(codeGroup);
    }

    @Override
    public void delete(CodeGroup codeGroup) {
        springDataRepository.delete(codeGroup);
    }

}
