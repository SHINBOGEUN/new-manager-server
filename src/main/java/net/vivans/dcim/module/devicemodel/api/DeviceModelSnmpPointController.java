package net.vivans.dcim.module.devicemodel.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelSnmpPointBulkCreateRequest;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelSnmpPointCreateRequest;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelSnmpPointResponse;
import net.vivans.dcim.module.devicemodel.application.DeviceModelSnmpPointQueryService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points")
@Tag(name = "device-model-snmp-point", description = "장비 모델 SNMP 수집 point CRUD API")
public class DeviceModelSnmpPointController {

    private final DeviceModelSnmpPointQueryService deviceModelSnmpPointQueryService;

    @GetMapping
    @Operation(summary = "SNMP 수집 POINT 목록 조회 API", description = "해당 모델·프로토콜의 point를 id 오름차순으로 반환합니다.")
    public ResponseEntity<ApiResponse<List<DeviceModelSnmpPointResponse>>> getDeviceModelSnmpPoints(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId,
            @Parameter(description = "모델 프로토콜 ID (SNMP)") @PathVariable Integer protocolId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceModelSnmpPointQueryService.getDeviceModelSnmpPoints(modelId, protocolId)));
    }

    @GetMapping("/{pointId}")
    @Operation(summary = "SNMP 수집 POINT 단건 조회 API")
    public ResponseEntity<ApiResponse<DeviceModelSnmpPointResponse>> getDeviceModelSnmpPoint(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId,
            @Parameter(description = "모델 프로토콜 ID (SNMP)") @PathVariable Integer protocolId,
            @Parameter(description = "SNMP point ID") @PathVariable Integer pointId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceModelSnmpPointQueryService.getDeviceModelSnmpPoint(modelId, protocolId, pointId)));
    }

    @PostMapping
    @Operation(summary = "SNMP 수집 POINT 등록 API")
    public ResponseEntity<ApiResponse<DeviceModelSnmpPointResponse>> createDeviceModelSnmpPoint(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId,
            @Parameter(description = "모델 프로토콜 ID (SNMP)") @PathVariable Integer protocolId,
            @Valid @RequestBody DeviceModelSnmpPointCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceModelSnmpPointQueryService.createDeviceModelSnmpPoint(modelId, protocolId, request)));
    }

    @PostMapping("/bulk")
    @Operation(
            summary = "SNMP 수집 POINT 일괄 등록 API",
            description = "여러 OID를 한 요청으로 등록합니다. 하나라도 실패하면 전부 롤백되며, 수집 스크립트는 1회만 재생성합니다."
    )
    public ResponseEntity<ApiResponse<List<DeviceModelSnmpPointResponse>>> createDeviceModelSnmpPoints(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId,
            @Parameter(description = "모델 프로토콜 ID (SNMP)") @PathVariable Integer protocolId,
            @Valid @RequestBody DeviceModelSnmpPointBulkCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceModelSnmpPointQueryService.createDeviceModelSnmpPoints(modelId, protocolId, request)));
    }

    @PutMapping("/{pointId}")
    @Operation(summary = "SNMP 수집 POINT 수정 API", description = "요청 body로 전체 교체합니다.")
    public ResponseEntity<ApiResponse<DeviceModelSnmpPointResponse>> updateDeviceModelSnmpPoint(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId,
            @Parameter(description = "모델 프로토콜 ID (SNMP)") @PathVariable Integer protocolId,
            @Parameter(description = "SNMP point ID") @PathVariable Integer pointId,
            @Valid @RequestBody DeviceModelSnmpPointCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceModelSnmpPointQueryService.updateDeviceModelSnmpPoint(modelId, protocolId, pointId, request)));
    }

    @DeleteMapping("/{pointId}")
    @Operation(summary = "SNMP 수집 POINT 삭제 API")
    public ResponseEntity<ApiResponse<Integer>> deleteDeviceModelSnmpPoint(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId,
            @Parameter(description = "모델 프로토콜 ID (SNMP)") @PathVariable Integer protocolId,
            @Parameter(description = "SNMP point ID") @PathVariable Integer pointId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceModelSnmpPointQueryService.deleteDeviceModelSnmpPoint(modelId, protocolId, pointId)));
    }
}
