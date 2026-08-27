package net.vivans.dcim.module.common.domain.repository;

import jakarta.validation.constraints.NotBlank;
import net.vivans.dcim.module.common.domain.model.CommonCode;

import java.util.List;
import java.util.Optional;

public interface CommonCodeRepository {

    CommonCode save(CommonCode code);

    Optional<CommonCode> findById(Integer id);

    Optional<CommonCode> findByCodeGroupGroupKeyAndCode(String groupKey, String code);

    boolean existsByCodeGroupIdAndCode(Integer groupId, String code);

    boolean existsByCodeAndIdNot(@NotBlank(message = "code must not be empty") String code, Integer id);

    boolean existsByNameAndIdNot(@NotBlank(message = "name must not be empty") String name, Integer id);

    List<CommonCode> findAll();

    List<CommonCode> findByCodeGroupId(Integer codeGroupId);
}
