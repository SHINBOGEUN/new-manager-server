package net.vivans.dcim.module.lora.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.api.dto.DeviceModelLoraPointRequest;
import net.vivans.dcim.module.lora.api.dto.DeviceModelLoraPointResponse;
import net.vivans.dcim.module.lora.application.DeviceModelLoraPointService;
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
@RequestMapping("/api/manager/device-models/{modelId}/lora-points")
@Tag(name = "device-model-lora-point", description = "장비 모델 단위 LoRa payload 필드 매핑 CRUD API")
public class DeviceModelLoraPointController {

    private final DeviceModelLoraPointService deviceModelLoraPointService;

    @GetMapping
    @Operation(summary = "모델 LoRa 필드 매핑 목록 조회")
    public ResponseEntity<ApiResponse<List<DeviceModelLoraPointResponse>>> getAll(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer modelId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceModelLoraPointService.getAllByModelId(modelId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "모델 LoRa 필드 매핑 등록")
    public ResponseEntity<ApiResponse<DeviceModelLoraPointResponse>> create(
            @PathVariable Integer modelId,
            @Valid @RequestBody DeviceModelLoraPointRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceModelLoraPointService.create(modelId, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "모델 LoRa 필드 매핑 수정")
    public ResponseEntity<ApiResponse<DeviceModelLoraPointResponse>> update(
            @PathVariable Integer modelId,
            @PathVariable Integer id,
            @Valid @RequestBody DeviceModelLoraPointRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceModelLoraPointService.update(modelId, id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "모델 LoRa 필드 매핑 삭제")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer modelId,
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceModelLoraPointService.delete(modelId, id)));
    }
}
