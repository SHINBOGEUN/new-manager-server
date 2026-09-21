package net.vivans.dcim.module.devicemodel.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelModbusPointCreateRequest;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelModbusPointResponse;
import net.vivans.dcim.module.devicemodel.application.DeviceModelModbusPointQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import retrofit2.http.Path;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/device-models/{modelId}/protocols/{protocolId}/modbus-points")
@Tag(name = "device-model-modbus-point", description = "장비 모델 Modbus 수집 point CRUD API")
public class DeviceModelModbusPointController {

    private final DeviceModelModbusPointQueryService deviceModelModbusPointQueryService;

    @GetMapping
    @Operation(summary = "Modbus 수집 POINT 목록 조회 API", description = "해당 모델·프로토콜의 point를 id 오름차순으로 반환")
    public ResponseEntity<ApiResponse<List<DeviceModelModbusPointResponse>>> getDeviceModelModbusPoints(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId,
            @Parameter(description = "모델 프로토콜 ID (Modbus)") @PathVariable Integer protocolId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceModelModbusPointQueryService.getDeviceModelModbusPoints(modelId, protocolId)
        ));
    }

    @GetMapping("/{pointId}")
    @Operation(summary = "Modbus 수집 POINT 단건 조회 API")
    public ResponseEntity<ApiResponse<DeviceModelModbusPointResponse>> getDeviceModelModbusPoint(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId,
            @Parameter(description = "모델 프로토콜 ID (Modbus)") @PathVariable Integer protocolId,
            @Parameter(description = "Modbus point ID") @PathVariable Integer pointId
        ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceModelModbusPointQueryService.getDeviceModelModbusPoint(modelId, protocolId, pointId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Modbus 수집 POINT 등록 API")
    public ResponseEntity<ApiResponse<DeviceModelModbusPointResponse>> createDeviceModelModbusPoint(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId,
            @Parameter(description = "모델 프로토콜 ID (Modbus)") @PathVariable Integer protocolId,
            @Valid @RequestBody DeviceModelModbusPointCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceModelModbusPointQueryService.createDeviceModelModbusPoint(modelId, protocolId, request)
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{pointId}")
    @Operation(summary = "Modbus 수집 POINT 수정 API")
    public ResponseEntity<ApiResponse<DeviceModelModbusPointResponse>> updateDeviceModelModbusPoint(
        @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId,
        @Parameter(description = "모델 프로토콜 ID (Modbus)") @PathVariable Integer protocolId,
        @Parameter(description = "Modbus point ID") @PathVariable Integer pointId,
        @Valid @RequestBody DeviceModelModbusPointCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceModelModbusPointQueryService.updateDeviceModelModbusPoint(modelId, protocolId, pointId, request)
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{pointId}")
    @Operation(summary = "Modbus 수집 POINT 삭제 API")
    public ResponseEntity<ApiResponse<Integer>> deleteDeviceModelModbusPoint(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId,
            @Parameter(description = "모델 프로토콜") @PathVariable Integer protocolId,
            @Parameter(description = "Modbus point ID") @PathVariable Integer pointId) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceModelModbusPointQueryService.deleteDeviceModelModbusPoint(modelId, protocolId, pointId)
        ));
    }
}
