package net.vivans.dcim.module.devicemodel.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DeviceModelSnmpPointBulkCreateRequest(
        @Schema(description = "등록할 SNMP point 목록")
        @NotEmpty(message = "points must not be empty")
        @Valid
        List<DeviceModelSnmpPointCreateRequest> points
) {
}
