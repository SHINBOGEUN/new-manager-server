package net.vivans.dcim.module.lora.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoraDataPointTypeResolver {

    private static final String DATA_POINT_TYPE_GROUP = "DATA_POINT_TYPE";
    private static final String DEFAULT_DATA_POINT_TYPE = "UNCLASSIFIED";

    private final CommonCodeRepository commonCodeRepository;

    public CommonCode resolve(Integer id) {
        if (id == null) {
            return commonCodeRepository
                    .findByCodeGroupGroupKeyAndCode(DATA_POINT_TYPE_GROUP, DEFAULT_DATA_POINT_TYPE)
                    .orElseThrow(() -> new EntityNotFoundException(
                            DATA_POINT_TYPE_GROUP + "/" + DEFAULT_DATA_POINT_TYPE + " is not configured"));
        }
        CommonCode code = commonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + id));
        if (!DATA_POINT_TYPE_GROUP.equals(code.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("dataPointTypeId must belong to DATA_POINT_TYPE");
        }
        return code;
    }
}
