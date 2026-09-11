package net.vivans.dcim.module.common.application;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.api.dto.CommonCodeRequest;
import net.vivans.dcim.module.common.api.dto.CommonCodeResponse;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CodeGroupRepository;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommonCodeQueryService {

    private final CommonCodeRepository commonCodeRepository;
    private final CodeGroupRepository codeGroupRepository;

    @Transactional
    public CommonCodeResponse createCommonCode(CommonCodeRequest request) {
        CodeGroup codeGroup = codeGroupRepository.findById(request.groupId())
                .orElseThrow(() -> new EntityNotFoundException("CodeGroup not found: " + request.groupId()));

        if (commonCodeRepository.existsByCodeGroupIdAndCode(codeGroup.getId(), request.code())) {
            throw new IllegalArgumentException("Code already exists in this group");
        }

        CommonCode code = CommonCode.createCommonCode(
                codeGroup, request.code(), request.name(), request.sortOrder());
        commonCodeRepository.save(code);
        return CommonCodeResponse.from(code);
    }

    @Transactional
    public CommonCodeResponse updateCommonCode(Integer id, CommonCodeRequest request) {
        CommonCode code = commonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + id));
        CodeGroup codeGroup = codeGroupRepository.findById(request.groupId())
                .orElseThrow(() -> new EntityNotFoundException("CodeGroup not found: " + request.groupId()));

        if (commonCodeRepository.existsByCodeGroupIdAndCodeAndIdNot(codeGroup.getId(), request.code(), id)) {
            throw new IllegalArgumentException("Code already exists in this group");
        }
        code.update(codeGroup, request.code(), request.name(), request.sortOrder());

        return CommonCodeResponse.from(commonCodeRepository.save(code));
    }

    @Transactional
    public Integer deleteCommonCode(Integer id) {
        CommonCode code = commonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + id));
        try {
            commonCodeRepository.delete(code);
            return id;
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("공통 코드가 다른 설정에서 사용 중이라 삭제할 수 없습니다.");
        }
    }

    public List<CommonCodeResponse> getCommonCodeList(Integer codeGroupId) {
        if (codeGroupId == null) {
            return commonCodeRepository.findAll().stream().map(CommonCodeResponse::from).toList();
        }
        codeGroupRepository.findById(codeGroupId)
                .orElseThrow(() -> new EntityNotFoundException("CodeGroup not found: " + codeGroupId));
        return commonCodeRepository.findByCodeGroupId(codeGroupId).stream()
                .map(CommonCodeResponse::from)
                .toList();
    }
}
