package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;


/**
 * PUT 전용 요청. Create와 달리 enabled를 필수로 받는다 —
 * 전체 교체 시 생략된 enabled가 기본값 true로 덮여 꺼둔 회선이 켜지는 것을 막기 위함.
 */
public record DeviceModbusReadingUpdateRequest(
        @Schema(description = "device_model_modbus_point.id")
        @NotNull(message = "pointId is required")
        @Positive
        Integer pointId,

        @Schema(description = "회선의 Modbus unit/slave ID", example = "0")
        @NotNull(message = "unitId is required")
        @Min(0)
        @Max(247)
        Integer unitId,

        @Schema(
                description = "보정이 끝난 실제 Modbus 요청 시작 주소",
                example = "11265"
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

        @Schema(description = "사용 여부", example = "true")
        @NotNull(message = "enabled is required")
        Boolean enabled
) {
}