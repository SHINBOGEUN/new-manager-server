package net.vivans.dcim.module.lora.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraPointOverrideRequest;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraPointOverrideResponse;
import net.vivans.dcim.module.lora.application.DeviceLoraPointOverrideService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/manager/devices/{deviceId}/lora-point-overrides")
@Tag(name = "device-lora-point-override", description = "장비 단위 LoRa payload 필드 매핑 예외(override) CRUD API")
public class DeviceLoraPointOverrideController {

    private final DeviceLoraPointOverrideService deviceLoraPointOverrideService;

    @GetMapping
    @Operation(summary = "장비 LoRa override 목록 조회")
    public ResponseEntity<ApiResponse<List<DeviceLoraPointOverrideResponse>>> getAll(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceLoraPointOverrideService.getAllByDeviceId(deviceId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "장비 LoRa override 등록")
    public ResponseEntity<ApiResponse<DeviceLoraPointOverrideResponse>> create(
            @PathVariable Integer deviceId,
            @Valid @RequestBody DeviceLoraPointOverrideRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceLoraPointOverrideService.create(deviceId, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "장비 LoRa override 수정")
    public ResponseEntity<ApiResponse<DeviceLoraPointOverrideResponse>> update(
            @PathVariable Integer deviceId,
            @PathVariable Integer id,
            @Valid @RequestBody DeviceLoraPointOverrideRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceLoraPointOverrideService.update(deviceId, id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "장비 LoRa override 삭제")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer deviceId,
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceLoraPointOverrideService.delete(deviceId, id)));
    }
}
