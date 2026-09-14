package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceAssetDetailResponse;
import net.vivans.dcim.module.device.api.dto.DeviceAssetHistoryResponse;
import net.vivans.dcim.module.device.api.dto.DeviceAssetStatusTransitionRequest;
import net.vivans.dcim.module.device.api.dto.DeviceAssetSummaryResponse;
import net.vivans.dcim.module.device.api.dto.DeviceResponse;
import net.vivans.dcim.module.device.api.dto.DeviceImageResponse;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementRequest;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementResponse;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementHistoryResponse;
import net.vivans.dcim.module.device.api.dto.RackLayoutResponse;
import net.vivans.dcim.module.device.application.DeviceAssetService;
import net.vivans.dcim.shared.api.ApiResponse;
import net.vivans.dcim.shared.api.PageResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/assets")
@Tag(name = "asset", description = "장비 자산·Rack 배치·이미지 관리 API")
public class DeviceAssetController {
    private final DeviceAssetService assetService;

    @GetMapping
    @Operation(summary = "자산 목록 조회")
    public ResponseEntity<ApiResponse<PageResponse<DeviceAssetSummaryResponse>>> getAssets(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.getAssets(name, page, size)));
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceAssetDetailResponse>> getAsset(@PathVariable Integer deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.getAsset(deviceId)));
    }

    @GetMapping("/{deviceId}/history")
    @Operation(summary = "장비 자산 변경 이력 조회")
    public ResponseEntity<ApiResponse<java.util.List<DeviceAssetHistoryResponse>>> getAssetHistory(
            @PathVariable Integer deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.getAssetHistory(deviceId)));
    }

    @PostMapping("/{deviceId}/status")
    @Operation(summary = "자산 운영 상태 전환", description = "미사용·폐기 상태는 장비 수집과 장비 기반 위젯 대상에서 제외됩니다.")
    public ResponseEntity<ApiResponse<DeviceResponse>> transitionAssetStatus(
            @PathVariable Integer deviceId, @Valid @RequestBody DeviceAssetStatusTransitionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.transitionAssetStatus(deviceId, request)));
    }

    @PutMapping("/{deviceId}/rack-placement")
    public ResponseEntity<ApiResponse<DeviceRackPlacementResponse>> saveRackPlacement(
            @PathVariable Integer deviceId, @Valid @RequestBody DeviceRackPlacementRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.saveRackPlacement(deviceId, request)));
    }

    @GetMapping("/{deviceId}/rack-history")
    @Operation(summary = "장비 Rack 배치 변경 이력 조회")
    public ResponseEntity<ApiResponse<java.util.List<DeviceRackPlacementHistoryResponse>>> getRackPlacementHistory(
            @PathVariable Integer deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.getRackPlacementHistory(deviceId)));
    }

    @DeleteMapping("/{deviceId}/rack-placement")
    public ResponseEntity<ApiResponse<Integer>> deleteRackPlacement(@PathVariable Integer deviceId) {
        assetService.deleteRackPlacement(deviceId);
        return ResponseEntity.ok(ApiResponse.ok(deviceId));
    }

    @GetMapping("/racks/{rackLocationCode}/layout")
    public ResponseEntity<ApiResponse<RackLayoutResponse>> getRackLayout(@PathVariable String rackLocationCode) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.getRackLayout(rackLocationCode)));
    }

    @PostMapping(path = "/{deviceId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DeviceImageResponse>> uploadImage(
            @PathVariable Integer deviceId, @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean primary) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.uploadImage(deviceId, file, primary)));
    }

    @GetMapping("/{deviceId}/images/{imageId}/content")
    public ResponseEntity<Resource> getImage(@PathVariable Integer deviceId, @PathVariable Integer imageId) {
        DeviceImageResponse image = assetService.getImage(deviceId, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.originalName().replace("\"", "") + "\"")
                .body(assetService.loadImage(deviceId, imageId));
    }

    @DeleteMapping("/{deviceId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Integer>> deleteImage(@PathVariable Integer deviceId, @PathVariable Integer imageId) {
        assetService.deleteImage(deviceId, imageId);
        return ResponseEntity.ok(ApiResponse.ok(imageId));
    }
}
