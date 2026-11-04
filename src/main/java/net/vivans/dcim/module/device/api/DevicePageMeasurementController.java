package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DevicePageDevicesRequest;
import net.vivans.dcim.module.device.api.dto.DevicePageDisplayResponse;
import net.vivans.dcim.module.device.api.dto.DevicePageDisplayUpdateRequest;
import net.vivans.dcim.module.device.api.dto.DevicePageMeasurementsResponse;
import net.vivans.dcim.module.device.application.DevicePageDisplayService;
import net.vivans.dcim.module.device.application.DevicePageMeasurementService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/device-pages")
@Tag(name = "device-page-measurements", description = "페이지별 장비 측정항목 및 최신값 API")
public class DevicePageMeasurementController {
    private final DevicePageMeasurementService service;
    private final DevicePageDisplayService displayService;

    @GetMapping("/{pageCode}/display")
    @Operation(summary = "페이지 표시 방식 및 장면 배치 조회")
    public ResponseEntity<ApiResponse<DevicePageDisplayResponse>> getDisplay(@PathVariable String pageCode) {
        return ResponseEntity.ok(ApiResponse.ok(displayService.get(pageCode)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{pageCode}/display")
    @Operation(summary = "페이지 표시 방식 및 장면 배치 저장",
            description = "DATA, PLAN_2D, SCENE_3D 모드와 랙·장비 위치, 랙에 장착한 측정 포인트 센서를 저장합니다.")
    public ResponseEntity<ApiResponse<DevicePageDisplayResponse>> updateDisplay(
            @PathVariable String pageCode,
            @Valid @RequestBody DevicePageDisplayUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(displayService.update(pageCode, request)));
    }

    @GetMapping("/{pageCode}/devices")
    @Operation(summary = "페이지에 선택된 장비 ID 목록 조회")
    public ResponseEntity<ApiResponse<List<Integer>>> getDevices(@PathVariable String pageCode) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDeviceIds(pageCode)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{pageCode}/devices")
    @Operation(summary = "페이지에 표시할 장비 선택", description = "deviceIds 전체를 교체합니다. 빈 배열이면 선택을 해제합니다.")
    public ResponseEntity<ApiResponse<List<Integer>>> replaceDevices(
            @PathVariable String pageCode,
            @Valid @RequestBody DevicePageDevicesRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.replaceDevices(pageCode, request.deviceIds())));
    }

    @GetMapping("/{pageCode}/measurements")
    @Operation(summary = "페이지 장비의 모든 등록 측정항목과 최신값 조회",
            description = "값이 없는 항목도 null 값으로 포함합니다. SNMP·Modbus·LoRa(MQTT) 값을 함께 반환합니다.")
    public ResponseEntity<ApiResponse<DevicePageMeasurementsResponse>> getMeasurements(
            @PathVariable String pageCode,
            @Parameter(description = "최신값 검색 범위(시간), 1~720. 기본 168")
            @RequestParam(defaultValue = "168") int lookbackHours) {
        return ResponseEntity.ok(ApiResponse.ok(service.getMeasurements(pageCode, lookbackHours)));
    }
}
