package net.vivans.dcim.module.common.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommonCodeJpaRepository implements CommonCodeRepository {

    private final CommonCodeSpringDataRepository springDataRepository;

    @Override
    public CommonCode save(CommonCode code) {
        return springDataRepository.save(code);
    }

    @Override
    public Optional<CommonCode> findById(Integer id){
        return springDataRepository.findById(id);
    }

    @Override
    public Optional<CommonCode> findByCodeGroupGroupKeyAndCode(String groupKey, String code) {
        return springDataRepository.findByCodeGroup_GroupKeyAndCode(groupKey, code);
    }

    @Override
    public boolean existsByCodeGroupIdAndCode(Integer groupId, String code) {
        return springDataRepository.existsByCodeGroupIdAndCode(groupId, code);
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, Integer id) {
        return springDataRepository.existsByCodeAndIdNot(code, id);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Integer id) {
        return springDataRepository.existsByNameAndIdNot(name, id);
    }

    @Override
    public List<CommonCode> findAll() {
        return springDataRepository.findAll();
    }

    @Override
    public List<CommonCode> findByCodeGroupId(Integer codeGroupId) {
        return springDataRepository.findByCodeGroupId(codeGroupId);
    }
}
