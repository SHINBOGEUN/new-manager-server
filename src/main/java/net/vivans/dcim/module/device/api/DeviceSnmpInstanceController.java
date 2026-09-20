package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceSnmpInstanceCreateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceSnmpInstanceResponse;
import net.vivans.dcim.module.device.application.DeviceSnmpInstanceQueryService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance")
@Tag(name = "device-snmp-instance", description = "장비 SNMP instance 인덱스 API")
public class DeviceSnmpInstanceController {

    private final DeviceSnmpInstanceQueryService deviceSnmpInstanceQueryService;

    @GetMapping
    @Operation(summary = "SNMP instance 조회 API",
            description = "endpoint에 등록된 instance 단건. 미등록이면 404.")
    public ResponseEntity<ApiResponse<DeviceSnmpInstanceResponse>> getSnmpInstance(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "엔드포인트 ID") @PathVariable Integer endpointId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceSnmpInstanceQueryService.getSnmpInstance(deviceId, endpointId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "SNMP instance 등록 API",
            description = "SNMP endpoint당 1건. OID {instanceId} 치환용. 모델에 requiresInstance point가 있을 때만.")
    public ResponseEntity<ApiResponse<DeviceSnmpInstanceResponse>> createSnmpInstance(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "엔드포인트 ID") @PathVariable Integer endpointId,
            @Valid @RequestBody DeviceSnmpInstanceCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceSnmpInstanceQueryService.createSnmpInstance(deviceId, endpointId, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    @Operation(summary = "SNMP instance 수정 API",
            description = "instanceId 전체 교체. 미등록이면 404.")
    public ResponseEntity<ApiResponse<DeviceSnmpInstanceResponse>> updateSnmpInstance(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "엔드포인트 ID") @PathVariable Integer endpointId,
            @Valid @RequestBody DeviceSnmpInstanceCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceSnmpInstanceQueryService.updateSnmpInstance(deviceId, endpointId, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    @Operation(summary = "SNMP instance 삭제 API",
            description = "endpoint의 instance 행 삭제. 미등록이면 404.")
    public ResponseEntity<ApiResponse<Integer>> deleteSnmpInstance(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "엔드포인트 ID") @PathVariable Integer endpointId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceSnmpInstanceQueryService.deleteSnmpInstance(deviceId, endpointId)));
    }
}
