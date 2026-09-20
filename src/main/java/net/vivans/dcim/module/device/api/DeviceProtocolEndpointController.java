package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceProtocolEndpointCreateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceProtocolEndpointResponse;
import net.vivans.dcim.module.device.application.DeviceProtocolEndpointQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/manager/devices/{deviceId}/endpoints")
@Tag(name = "device-protocol-endpoint", description = "장비 프로토콜 엔드포인트 CRUD API")
public class DeviceProtocolEndpointController {

    private final DeviceProtocolEndpointQueryService deviceProtocolEndpointQueryService;

    @GetMapping
    @Operation(summary = "프로토콜 엔드포인트 목록 조회 API", description = "해당 장비의 endpoint를 id 오름차순으로 반환합니다.")
    public ResponseEntity<ApiResponse<List<DeviceProtocolEndpointResponse>>> getEndpoints(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceProtocolEndpointQueryService.getEndpoints(deviceId)));
    }

    @GetMapping("/{endpointId}")
    @Operation(summary = "프로토콜 엔드포인트 단건 조회 API")
    public ResponseEntity<ApiResponse<DeviceProtocolEndpointResponse>> getEndpoint(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "엔드포인트 ID") @PathVariable Integer endpointId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceProtocolEndpointQueryService.getEndpoint(deviceId, endpointId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "프로토콜 엔드포인트 등록 API", description = "장비당 프로토콜 타입 1건. host/port 공통 전송층.")
    public ResponseEntity<ApiResponse<DeviceProtocolEndpointResponse>> createEndpoint(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Valid @RequestBody DeviceProtocolEndpointCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceProtocolEndpointQueryService.createEndpoint(deviceId, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{endpointId}")
    @Operation(summary = "프로토콜 엔드포인트 수정 API", description = "요청 body는 등록과 동일하며 전체 교체입니다.")
    public ResponseEntity<ApiResponse<DeviceProtocolEndpointResponse>> updateEndpoint(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "엔드포인트 ID") @PathVariable Integer endpointId,
            @Valid @RequestBody DeviceProtocolEndpointCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceProtocolEndpointQueryService.updateEndpoint(deviceId, endpointId, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{endpointId}")
    @Operation(summary = "프로토콜 엔드포인트 삭제 API")
    public ResponseEntity<ApiResponse<Integer>> deleteEndpoint(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "엔드포인트 ID") @PathVariable Integer endpointId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceProtocolEndpointQueryService.deleteEndpoint(deviceId, endpointId)));
    }
}
