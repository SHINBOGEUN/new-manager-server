package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceMonitoringPolicyRequest;
import net.vivans.dcim.module.device.api.dto.DeviceMonitoringPolicyResponse;
import net.vivans.dcim.module.device.application.DeviceMonitoringPolicyService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/devices/{deviceId}/monitoring-policy")
public class DeviceMonitoringPolicyController {
    private final DeviceMonitoringPolicyService service;

    @GetMapping
    @Operation(summary = "장비별 데이터 감시 정책 조회")
    public ResponseEntity<ApiResponse<DeviceMonitoringPolicyResponse>> get(@PathVariable Integer deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(deviceId)));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "장비별 데이터 감시 정책 저장")
    public ResponseEntity<ApiResponse<DeviceMonitoringPolicyResponse>> save(
            @PathVariable Integer deviceId, @Valid @RequestBody DeviceMonitoringPolicyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.save(deviceId, request)));
    }
}
