package net.vivans.dcim.module.lora.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointRequest;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointResponse;
import net.vivans.dcim.module.lora.application.DeviceLoraEndpointService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/lora/endpoints")
@Tag(name = "lora-endpoint", description = "LoRa/Dragino 외부 식별자(devEUI/deviceName) ↔ device 매칭 CRUD API")
public class DeviceLoraEndpointController {

    private final DeviceLoraEndpointService deviceLoraEndpointService;

    @GetMapping
    @Operation(summary = "LoRa 외부 식별자 목록 조회", description = "deviceId로 필터링할 수 있습니다.")
    public ResponseEntity<ApiResponse<List<DeviceLoraEndpointResponse>>> getAll(
            @Parameter(description = "장비 ID (선택)") @RequestParam(required = false) Integer deviceId
    ) {
        List<DeviceLoraEndpointResponse> result = deviceId == null
                ? deviceLoraEndpointService.getAll()
                : deviceLoraEndpointService.getAllByDeviceId(deviceId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "LoRa 외부 식별자 등록")
    public ResponseEntity<ApiResponse<DeviceLoraEndpointResponse>> create(
            @Valid @RequestBody DeviceLoraEndpointRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceLoraEndpointService.create(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "LoRa 외부 식별자 수정")
    public ResponseEntity<ApiResponse<DeviceLoraEndpointResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody DeviceLoraEndpointRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceLoraEndpointService.update(id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "LoRa 외부 식별자 삭제")
    public ResponseEntity<ApiResponse<Integer>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(deviceLoraEndpointService.delete(id)));
    }
}
