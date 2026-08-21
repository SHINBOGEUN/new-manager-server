package net.vivans.dcim.module.live.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.live.api.dto.LiveDeviceResponse;
import net.vivans.dcim.module.live.api.dto.LiveSelectionRequest;
import net.vivans.dcim.module.live.api.dto.LiveSelectionResponse;
import net.vivans.dcim.module.live.application.LiveTelemetryQueryService;
import net.vivans.dcim.module.live.application.LiveTelemetrySelectionService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/live")
@Tag(name = "live-telemetry", description = "SNMP 실시간 수집 대상 선택 API")
public class LiveTelemetryController {

    private final LiveTelemetryQueryService liveTelemetryQueryService;
    private final LiveTelemetrySelectionService liveTelemetrySelectionService;

    @GetMapping("/devices")
    @Operation(
            summary = "실시간 선택 가능 장비·point 목록",
            description = "SNMP endpoint와 수집 가능한 point가 있는 장비만 반환합니다. Modbus는 포함하지 않습니다."
    )
    public ResponseEntity<ApiResponse<List<LiveDeviceResponse>>> getSelectableDevices() {
        return ResponseEntity.ok(ApiResponse.ok(liveTelemetryQueryService.getSelectableDevices()));
    }

    @GetMapping("/selection")
    @Operation(summary = "현재 실시간 수집 대상 조회")
    public ResponseEntity<ApiResponse<LiveSelectionResponse>> getSelection() {
        return ResponseEntity.ok(ApiResponse.ok(liveTelemetrySelectionService.getSelection()));
    }

    @PutMapping("/selection")
    @Operation(
            summary = "실시간 수집 대상 설정",
            description = "장비별 point를 선택합니다. 빈 목록이면 수집을 중지합니다. 세션은 기본 30분 후 만료됩니다."
    )
    public ResponseEntity<ApiResponse<LiveSelectionResponse>> putSelection(
            @Valid @RequestBody LiveSelectionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(liveTelemetrySelectionService.updateSelection(request)));
    }

    @DeleteMapping("/selection")
    @Operation(summary = "실시간 수집 대상 초기화")
    public ResponseEntity<ApiResponse<LiveSelectionResponse>> clearSelection() {
        return ResponseEntity.ok(ApiResponse.ok(liveTelemetrySelectionService.clearSelection()));
    }
}
