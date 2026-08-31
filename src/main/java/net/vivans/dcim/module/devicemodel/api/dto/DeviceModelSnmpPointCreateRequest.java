package net.vivans.dcim.module.devicemodel.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DeviceModelSnmpPointCreateRequest(
        @Schema(description = "식별자·표시명", example = "PRI-FLOW")
        @NotBlank(message = "name must not be empty")
        String name,

        @Schema(description = "OID 또는 {instanceId} 템플릿", example = "1.3.6.1.4.1.12345.{instanceId}.10.1.0")
        @NotBlank(message = "oid must not be empty")
        String oid,

        @Schema(description = "OID {instanceId} 치환 필요 여부 (기본 false)", example = "true")
        Boolean requiresInstance,

        @Schema(description = "단위", example = "L/min")
        String unit,

        @Schema(description = "원시값 배율 (null이면 1). 예: raw/10 → 0.1", example = "0.1")
        Double scale,

        @Schema(description = "사용 여부 (기본 true)", example = "true")
        Boolean enabled
) {
}
