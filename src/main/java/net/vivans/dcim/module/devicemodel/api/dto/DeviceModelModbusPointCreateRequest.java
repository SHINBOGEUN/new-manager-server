package net.vivans.dcim.module.devicemodel.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusByteOrder;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusDataType;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusRegisterType;

public record DeviceModelModbusPointCreateRequest(
        @Schema(description = "식별자·표시명", example = "TOTAL_WT")
        @NotBlank(message = "name must not be empty")
        String name,

        @Schema(description = "DATA_POINT_TYPE 그룹 common_code.id", example = "1")
        Integer dataPointTypeId,

        @Schema(description = "CATEGORY 그룹 common_code.id (분석 그래프 분류)", example = "13")
        Integer categoryCodeId,

        @Schema(description = "UNIT 그룹 common_code.id. 단위가 없으면 null", example = "53")
        Integer unitCodeId,

        @Schema(description = "레지스터 종류 (COIL/DISCRETE/HOLDING/INPUT)", example = "HOLDING")
        @NotNull(message = "registerType must not be null")
        ModbusRegisterType registerType,

        @Schema(description = "값 해석 타입 (INT16/UINT16/INT32/UINT32/FLOAT32)", example = "FLOAT32")
        @NotNull(message = "dataType must not be null")
        ModbusDataType dataType,

        @Schema(description = "멀티 레지스터 바이트 순서 (ABCD/CDAB/BADC/DCBA), 단일이면 null",
        example = "CDAB")
        ModbusByteOrder byteOrder,

        @Schema(description = "레지스터 주소 (0~65535). requiresInstance=true면 null", example = "11667")
        Integer address,

        @Schema(description = "주소를 인스턴스가 제공하는지 (기본 false)", example = "false")
        Boolean requiresInstance,

        @Schema(description = "원시값에 곱할 배율 (null이면 1)", example = "1000")
        Double scale,

        @Schema(description = "배율 적용 후 더할 값 (null이면 0). 최종값 = raw × scale + offset", example = "-50")
        Double offset,

        @Schema(description = "사용여부 (기본 true)", example = "true")
        Boolean enabled,
        String phase
) {
}
