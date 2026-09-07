package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DeviceEndpointModbusCreateRequest(
        @Schema(description = "Modbus unit/slave ID (0-247). 회선별로 다르면 null", example = "1")
        @Min(value = 0, message = "unitId must be greater than or equal to 0")
        @Max(value = 247, message = "unitId must be less than or equal to 247")
        Integer unitId
) {
}
