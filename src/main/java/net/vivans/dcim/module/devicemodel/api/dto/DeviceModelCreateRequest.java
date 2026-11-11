package net.vivans.dcim.module.devicemodel.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DeviceModelCreateRequest(
        @Schema(description = "모델/제품명", example = "LHT65N-PIR")
        @NotBlank(message = "name must not be empty")
        String name,

        @Schema(description = "제조사", example = "Dragino")
        @NotBlank(message = "manufacturer must not be empty")
        String manufacturer,

        @Schema(description = "모델 유형 ID (MODEL_TYPE)", example = "13")
        @NotNull(message = "deviceTypeId must not be null")
        Integer deviceTypeId,

        @Schema(description = "설명", example = "동작 감지 센서")
        String description,

        @Schema(description = "지원 프로토콜 목록 (1개 이상)")
        @NotEmpty(message = "at least one protocol required")
        @Valid
        List<DeviceModelProtocolRequest> protocols,
        @Schema(description = "PHASE_NOTATION 코드") String phaseNotation
) {
    public DeviceModelCreateRequest(String name, String manufacturer, Integer deviceTypeId, String description,
                                    List<DeviceModelProtocolRequest> protocols) {
        this(name, manufacturer, deviceTypeId, description, protocols, null);
    }
}
