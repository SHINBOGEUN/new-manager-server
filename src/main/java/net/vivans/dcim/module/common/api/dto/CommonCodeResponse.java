package net.vivans.dcim.module.common.api.dto;

import net.vivans.dcim.module.common.domain.model.CommonCode;

public record CommonCodeResponse(
        Integer id,
        Integer groupId,
        String code,
        String name,
        Integer sortOrder,
        boolean thresholdSupported
) {

    public static CommonCodeResponse from(CommonCode commonCode) {
        return new CommonCodeResponse(
                commonCode.getId(),
                commonCode.getCodeGroup().getId(),
                commonCode.getCode(),
                commonCode.getName(),
                commonCode.getSortOrder(),
                commonCode.isThresholdSupported());
    }
}
