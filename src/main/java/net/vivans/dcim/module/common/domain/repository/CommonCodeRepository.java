package net.vivans.dcim.module.common.domain.repository;

import net.vivans.dcim.module.common.domain.model.CommonCode;

import java.util.List;
import java.util.Optional;

public interface CommonCodeRepository {

    CommonCode save(CommonCode code);

    void delete(CommonCode code);

    Optional<CommonCode> findById(Integer id);

    Optional<CommonCode> findByCodeGroupGroupKeyAndCode(String groupKey, String code);

    boolean existsByCodeGroupIdAndCode(Integer groupId, String code);

    boolean existsByCodeGroupIdAndCodeAndIdNot(Integer groupId, String code, Integer id);

    boolean existsByCodeGroupId(Integer groupId);

    List<CommonCode> findAll();

    List<CommonCode> findByCodeGroupId(Integer codeGroupId);
}
