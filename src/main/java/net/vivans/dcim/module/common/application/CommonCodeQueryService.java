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
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
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
        validatePhaseNotationName(codeGroup, request.name());

        if (commonCodeRepository.existsByCodeGroupIdAndCode(codeGroup.getId(), request.code())) {
            throw new IllegalArgumentException("Code already exists in this group");
        }

        CommonCode code = CommonCode.createCommonCode(
                codeGroup, request.code(), request.name(), request.sortOrder(),
                Boolean.TRUE.equals(request.thresholdSupported()));
        commonCodeRepository.save(code);
        applyUnitConversion(code, request);
        return CommonCodeResponse.from(code);
    }

    @Transactional
    public CommonCodeResponse updateCommonCode(Integer id, CommonCodeRequest request) {
        CommonCode code = commonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + id));
        CodeGroup codeGroup = codeGroupRepository.findById(request.groupId())
                .orElseThrow(() -> new EntityNotFoundException("CodeGroup not found: " + request.groupId()));
        validatePhaseNotationName(codeGroup, request.name());

        if (commonCodeRepository.existsByCodeGroupIdAndCodeAndIdNot(codeGroup.getId(), request.code(), id)) {
            throw new IllegalArgumentException("Code already exists in this group");
        }
        boolean thresholdSupported = request.thresholdSupported() == null
                ? "DATA_POINT_TYPE".equals(codeGroup.getGroupKey()) && code.isThresholdSupported()
                : request.thresholdSupported();
        if (!"UNIT".equals(codeGroup.getGroupKey()) && code.getBaseUnit() != null)
            throw new IllegalArgumentException("환산 단위가 설정된 코드는 다른 그룹으로 이동할 수 없습니다");
        code.update(codeGroup, request.code(), request.name(), request.sortOrder(), thresholdSupported);
        applyUnitConversion(code, request);

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

    private void applyUnitConversion(CommonCode code, CommonCodeRequest request) {
        Integer id = request.baseUnitId();
        if (id == null && request.unitFactor() == null) return;
        boolean hasDependents = commonCodeRepository.findByCodeGroupId(code.getCodeGroup().getId()).stream()
                .anyMatch(other -> !other.getId().equals(code.getId()) && other.getBaseUnit() != null
                        && other.getBaseUnit().getId().equals(code.getId()));
        if (hasDependents && (!code.getId().equals(id) || request.unitFactor() == null
                || request.unitFactor().compareTo(java.math.BigDecimal.ONE) != 0))
            throw new IllegalArgumentException("다른 단위에서 사용하는 기준 단위는 변경할 수 없습니다");
        if (Integer.valueOf(0).equals(id) && request.unitFactor() == null) { code.setUnitConversion(null, null); return; }
        if (id == null || id <= 0 || request.unitFactor() == null) throw new IllegalArgumentException("기준 단위와 양수 배수를 함께 입력하세요");
        CommonCode base = id.equals(code.getId()) ? code : commonCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("기준 단위를 찾을 수 없습니다"));
        code.setUnitConversion(base, request.unitFactor());
    }

    private static void validatePhaseNotationName(CodeGroup codeGroup, String name) {
        if ("PHASE_NOTATION".equals(codeGroup.getGroupKey())) {
            DeviceModel.validatePhaseNotationFormat(name);
        }
    }
}
