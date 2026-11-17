package net.vivans.dcim.module.common.api.dto;

import net.vivans.dcim.module.common.domain.model.CommonCode;

public record CommonCodeResponse(
        Integer id,
        Integer groupId,
        String code,
        String name,
        Integer sortOrder,
        boolean thresholdSupported,
        Integer baseUnitId,
        String baseUnitCode,
        String baseUnitName,
        java.math.BigDecimal unitFactor
) {

    public static CommonCodeResponse from(CommonCode commonCode) {
        return new CommonCodeResponse(
                commonCode.getId(),
                commonCode.getCodeGroup().getId(),
                commonCode.getCode(),
                commonCode.getName(),
                commonCode.getSortOrder(),
                commonCode.isThresholdSupported(),
                commonCode.getBaseUnit() == null ? null : commonCode.getBaseUnit().getId(),
                commonCode.getBaseUnit() == null ? null : commonCode.getBaseUnit().getCode(),
                commonCode.getBaseUnit() == null ? null : commonCode.getBaseUnit().getName(),
                commonCode.getUnitFactor());
    }
}
