package net.vivans.dcim.module.devicegroup.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.devicegroup.api.dto.DeviceGroupRequest;
import net.vivans.dcim.module.devicegroup.api.dto.DeviceGroupResponse;
import net.vivans.dcim.module.devicegroup.application.DeviceGroupQueryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/device-groups")
@Tag(name = "device-groups", description = "물리 위치와 별개인 범용 장비 그룹 API")
public class DeviceGroupController {

    private final DeviceGroupQueryService deviceGroupQueryService;

    @GetMapping
    @Operation(summary = "장비 그룹 목록 조회 API")
    public ResponseEntity<ApiResponse<List<DeviceGroupResponse>>> getDeviceGroups(
            @Parameter(description = "사용 여부") @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(deviceGroupQueryService.getDeviceGroups(enabled)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "장비 그룹 단건 조회 API")
    public ResponseEntity<ApiResponse<DeviceGroupResponse>> getDeviceGroup(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(deviceGroupQueryService.getDeviceGroup(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "장비 그룹 생성 API")
    public ResponseEntity<ApiResponse<DeviceGroupResponse>> createDeviceGroup(
            @Valid @RequestBody DeviceGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(deviceGroupQueryService.createDeviceGroup(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "장비 그룹 수정 API", description = "deviceIds는 전체 교체입니다.")
    public ResponseEntity<ApiResponse<DeviceGroupResponse>> updateDeviceGroup(
            @PathVariable Integer id,
            @Valid @RequestBody DeviceGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(deviceGroupQueryService.updateDeviceGroup(id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "장비 그룹 삭제 API", description = "그룹 연결만 함께 제거하며 장비는 삭제하지 않습니다.")
    public ResponseEntity<ApiResponse<Integer>> deleteDeviceGroup(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(deviceGroupQueryService.deleteDeviceGroup(id)));
    }
}
