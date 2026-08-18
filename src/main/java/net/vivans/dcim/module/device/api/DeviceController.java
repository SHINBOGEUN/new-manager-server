package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceCapabilityResponse;
import net.vivans.dcim.module.device.api.dto.DeviceCreateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceResponse;
import net.vivans.dcim.module.device.application.DeviceCapabilityQueryService;
import net.vivans.dcim.module.device.application.DeviceQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import net.vivans.dcim.shared.api.PageResponse;
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
@RequestMapping("/api/manager/devices")
@Tag(name = "device", description = "DCIM 장비 CRUD API")
public class DeviceController {

    private final DeviceQueryService deviceQueryService;
    private final DeviceCapabilityQueryService deviceCapabilityQueryService;

    @GetMapping
    @Operation(summary = "장비 목록 조회 API", description = "필터·페이징 지원. 정렬은 id 오름차순.")
    public ResponseEntity<ApiResponse<PageResponse<DeviceResponse>>> getDevices(
            @Parameter(description = "모델 ID 일치") @RequestParam(required = false) Integer modelId,
            @Parameter(description = "위치 code 일치") @RequestParam(required = false) String locationNodeCode,
            @Parameter(description = "표시명 부분 일치") @RequestParam(required = false) String name,
            @Parameter(description = "사용 여부") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "노출 페이지 code (DEVICE_PAGE, 예: ENVIRONMENT)") @RequestParam(required = false) String pageCode,
            @Parameter(description = "페이지 번호 (1부터)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (기본 20, 최대 100)") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(deviceQueryService.getDevices(modelId, locationNodeCode, name, enabled, pageCode, page, size)));
    }

    @GetMapping("/capabilities")
    @Operation(
            summary = "장비 capabilities 조회 API",
            description = "pageCode·location으로 장비를 고르고 SNMP point OID·endpoint를 합성합니다."
    )
    public ResponseEntity<ApiResponse<List<DeviceCapabilityResponse>>> getCapabilities(
            @Parameter(description = "노출 페이지 code (DEVICE_PAGE, 예: ENVIRONMENT)") @RequestParam(required = false) String pageCode,
            @Parameter(description = "위치 code") @RequestParam(required = false) String locationNodeCode,
            @Parameter(description = "locationNodeCode 하위 트리 포함 여부") @RequestParam(required = false) Boolean includeSubtree
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                deviceCapabilityQueryService.getCapabilities(pageCode, locationNodeCode, includeSubtree)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "장비 단건 조회 API")
    public ResponseEntity<ApiResponse<DeviceResponse>> getDevice(
            @Parameter(description = "장비 ID") @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(deviceQueryService.getDevice(id)));
    }

    @PostMapping
    @Operation(summary = "장비 등록 API", description = "위치를 아직 모를 경우 locationNodeCode에 UNASSIGNED를 지정합니다.")
    public ResponseEntity<ApiResponse<DeviceResponse>> createDevice(
            @Valid @RequestBody DeviceCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(deviceQueryService.createDevice(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "장비 수정 API", description = "요청 body는 등록과 동일하며 전체 교체입니다.")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
            @Parameter(description = "장비 ID") @PathVariable Integer id,
            @Valid @RequestBody DeviceCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(deviceQueryService.updateDevice(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "장비 삭제 API")
    public ResponseEntity<ApiResponse<Integer>> deleteDevice(
            @Parameter(description = "장비 ID") @PathVariable Integer id) {
        deviceQueryService.deleteDevice(id);
        return ResponseEntity.ok(ApiResponse.ok(id));
    }
}
