package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DevicePageCreateRequest;
import net.vivans.dcim.module.device.api.dto.DevicePageReplaceRequest;
import net.vivans.dcim.module.device.api.dto.DevicePageResponse;
import net.vivans.dcim.module.device.application.DevicePageQueryService;
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
@RequestMapping("/api/manager/devices/{deviceId}/pages")
@Tag(name = "device-page", description = "장비↔노출 페이지 매핑 API")
public class DevicePageController {

    private final DevicePageQueryService devicePageQueryService;

    @GetMapping
    @Operation(summary = "장비 페이지 목록 조회")
    public ResponseEntity<ApiResponse<List<DevicePageResponse>>> getDevicePages(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(devicePageQueryService.getDevicePages(deviceId)));
    }

    @PostMapping
    @Operation(summary = "장비 페이지 단건 추가")
    public ResponseEntity<ApiResponse<DevicePageResponse>> createDevicePage(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Valid @RequestBody DevicePageCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(devicePageQueryService.createDevicePage(deviceId, request)));
    }

    @PutMapping
    @Operation(summary = "장비 페이지 전체 교체", description = "기존 매핑을 지우고 pageCodeIds로 교체. 빈 배열이면 전부 해제.")
    public ResponseEntity<ApiResponse<List<DevicePageResponse>>> replaceDevicePages(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Valid @RequestBody DevicePageReplaceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(devicePageQueryService.replaceDevicePages(deviceId, request)));
    }

    @DeleteMapping("/{pageId}")
    @Operation(summary = "장비 페이지 단건 삭제")
    public ResponseEntity<ApiResponse<Integer>> deleteDevicePage(
            @Parameter(description = "장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "매핑 ID") @PathVariable Integer pageId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(devicePageQueryService.deleteDevicePage(deviceId, pageId)));
    }
}
