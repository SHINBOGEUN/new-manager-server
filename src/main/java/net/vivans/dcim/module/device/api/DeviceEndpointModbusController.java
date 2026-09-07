package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceEndpointModbusCreateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceEndpointModbusResponse;
import net.vivans.dcim.module.device.application.DeviceEndpointModbusQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/devices/{deviceId}/endpoints/{endpointId}/modbus")
@Tag(name = "device-endpoint-modbus", description = "장비 Modbus 엔드포인트 설정 API")
public class DeviceEndpointModbusController {

    private final DeviceEndpointModbusQueryService deviceEndpointModbusQueryService;

    @GetMapping
    @Operation(summary = "Modbus 엔드포인트 설정 조회 API",
            description = "endpoint에 등록된 modbus 설정 단건. 미등록이면 404.")
    public ResponseEntity<ApiResponse<DeviceEndpointModbusResponse>> getEndpointModbus(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "엔드포인트 ID") @PathVariable Integer endpointId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceEndpointModbusQueryService.getEndpointModbus(deviceId, endpointId)
        ));
    }

    @PostMapping
    @Operation(summary = "Modbus 엔드포인트 설정 등록 API",
                description = "Modbus endpoint당 1건. unitId는 회선별로 다르면 null로 두고 매핑테이블에서 지정.")
    public ResponseEntity<ApiResponse<DeviceEndpointModbusResponse>> createEndpointModbus(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "엔드포인트 ID") @PathVariable Integer endpointId,
            @Valid @RequestBody DeviceEndpointModbusCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceEndpointModbusQueryService.createEndpointModbus(deviceId, endpointId, request)));
    }

    @PutMapping
    @Operation(summary = "Modbus 엔드포인트 설정 수정 API",
            description = "unitId 전체 교체. 미등록이면 404.")
    public ResponseEntity<ApiResponse<DeviceEndpointModbusResponse>> updateEndpointModbus(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "엔드포인트 ID") @PathVariable Integer endpointId,
            @Valid @RequestBody DeviceEndpointModbusCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceEndpointModbusQueryService.updateEndpointModbus(deviceId, endpointId, request)));
    }

    @DeleteMapping
    @Operation(summary = "Modbus 엔드포인트 설정 삭제 API",
            description = "endpoint의 modbus 설정 행 삭제. 미등록이면 404.")
    public ResponseEntity<ApiResponse<Integer>> deleteEndpointModbus(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "엔드포인트 ID") @PathVariable Integer endpointId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceEndpointModbusQueryService.deleteEndpointModbus(deviceId, endpointId)));
    }

}
