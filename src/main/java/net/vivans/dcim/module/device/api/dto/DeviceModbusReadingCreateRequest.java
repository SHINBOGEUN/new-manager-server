package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record DeviceModbusReadingCreateRequest(
        @Schema(description = "device_model_modbus_point.id")
        @NotNull(message = "pointId is required")
        @Positive
        Integer pointId,

        @Schema(description = "회선의 Modbus unit/slave ID", example = "1")
        @NotNull(message = "unitId is required")
        @Min(0)
        @Max(247)
        Integer unitId,

        @Schema(
                description = "보정이 끝난 실제 Modbus 요청 시작 주소",
                example = "11415"
        )
        @NotNull(message = "address is required")
        @Min(0)
        @Max(65535)
        Integer address,

        @Schema(description = "결과를 저장할 devices.id")
        @NotNull(message = "targetDeviceId is required")
        @Positive
        Integer targetDeviceId,

        @Schema(description = "결과를 저장할 필드명", example = "TOTAL_WT")
        @NotBlank(message = "pointName is required")
        @Size(max = 255)
        String pointName,

        @Schema(description = "사용 여부. 생략하면 true", example = "true")
        Boolean enabled
) {
}
