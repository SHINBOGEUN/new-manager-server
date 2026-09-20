package net.vivans.dcim.module.devicemodel.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelCreateRequest;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelResponse;
import net.vivans.dcim.module.devicemodel.application.DeviceModelQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/manager/device-models")
@Tag(name = "device-model", description = "장비 제품 모델 CRUD API")
public class DeviceModelController {

    private final DeviceModelQueryService deviceModelQueryService;

    @GetMapping
    @Operation(summary = "장비 모델 목록 조회 API", description = "전체 모델과 protocols[]를 반환합니다.")
    public ResponseEntity<ApiResponse<List<DeviceModelResponse>>> getDeviceModels(
            @Parameter(description = "모델명 부분 일치") @RequestParam(required = false) String name,
            @Parameter(description = "제조사 부분 일치") @RequestParam(required = false) String manufacturer) {
        return ResponseEntity.ok(ApiResponse.ok(deviceModelQueryService.getDeviceModels(name, manufacturer)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "장비 모델 단건 조회 API")
    public ResponseEntity<ApiResponse<DeviceModelResponse>> getDeviceModel(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(deviceModelQueryService.getDeviceModel(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "장비 모델 등록 API")
    public ResponseEntity<ApiResponse<DeviceModelResponse>> createDeviceModel(
            @Valid @RequestBody DeviceModelCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(deviceModelQueryService.createDeviceModel(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "장비 모델 수정 API", description = "protocols는 전체 교체됩니다.")
    public ResponseEntity<ApiResponse<DeviceModelResponse>> updateDeviceModel(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer id,
            @Valid @RequestBody DeviceModelCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(deviceModelQueryService.updateDeviceModel(id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "장비 모델 삭제 API")
    public ResponseEntity<ApiResponse<Integer>> deleteDeviceModel(
            @Parameter(description = "장비 모델 ID") @PathVariable Integer id) {
        deviceModelQueryService.deleteDeviceModel(id);
        return ResponseEntity.ok(ApiResponse.ok(id));
    }
}
