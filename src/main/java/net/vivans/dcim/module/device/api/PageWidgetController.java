package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.PageWidgetCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetEnabledRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetLayoutRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPueCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPueUpdateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPsychrometricCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPsychrometricUpdateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPowerDistributionCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPowerDistributionUpdateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetResponse;
import net.vivans.dcim.module.device.api.dto.PageWidgetUpdateRequest;
import net.vivans.dcim.module.device.application.PageWidgetQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/manager/widgets")
@Tag(name = "page-widget", description = "페이지 위젯 조회 정의 API")
public class PageWidgetController {

    private final PageWidgetQueryService pageWidgetQueryService;

    @GetMapping
    @Operation(summary = "페이지 위젯 목록")
    public ResponseEntity<ApiResponse<List<PageWidgetResponse>>> getWidgets(
            @Parameter(description = "DEVICE_PAGE code", example = "COOLING")
            @RequestParam String pageCode,
            @Parameter(description = "true/false 이면 enabled 필터")
            @RequestParam(required = false) Boolean enabled
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.getWidgets(pageCode, enabled)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "페이지 위젯 단건")
    public ResponseEntity<ApiResponse<PageWidgetResponse>> getWidget(
            @Parameter(description = "위젯 ID") @PathVariable Integer id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.getWidget(id)));
    }

    @PostMapping
    @Operation(summary = "페이지 위젯 등록")
    public ResponseEntity<ApiResponse<PageWidgetResponse>> createWidget(
            @Valid @RequestBody PageWidgetCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.createWidget(request)));
    }

    @PostMapping("/pue")
    @Operation(summary = "PUE 위젯 등록")
    public ResponseEntity<ApiResponse<PageWidgetResponse>> createPueWidget(
            @Valid @RequestBody PageWidgetPueCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.createPueWidget(request)));
    }

    @PutMapping("/{id}/pue")
    @Operation(summary = "PUE 위젯 수정")
    public ResponseEntity<ApiResponse<PageWidgetResponse>> updatePueWidget(
            @PathVariable Integer id,
            @Valid @RequestBody PageWidgetPueUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.updatePueWidget(id, request)));
    }

    @PostMapping("/psychrometric")
    @Operation(summary = "사이코메트릭 위젯 등록")
    public ResponseEntity<ApiResponse<PageWidgetResponse>> createPsychrometricWidget(
            @Valid @RequestBody PageWidgetPsychrometricCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.createPsychrometricWidget(request)));
    }

    @PutMapping("/{id}/psychrometric")
    @Operation(summary = "사이코메트릭 위젯 수정")
    public ResponseEntity<ApiResponse<PageWidgetResponse>> updatePsychrometricWidget(
            @PathVariable Integer id,
            @Valid @RequestBody PageWidgetPsychrometricUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.updatePsychrometricWidget(id, request)));
    }

    @PostMapping("/power-distribution")
    @Operation(summary = "전력 분배 위젯 등록", description = "모든 소스는 W 단위의 POWER 포인트여야 합니다.")
    public ResponseEntity<ApiResponse<PageWidgetResponse>> createPowerDistributionWidget(
            @Valid @RequestBody PageWidgetPowerDistributionCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.createPowerDistributionWidget(request)));
    }

    @PutMapping("/{id}/power-distribution")
    @Operation(summary = "전력 분배 위젯 수정")
    public ResponseEntity<ApiResponse<PageWidgetResponse>> updatePowerDistributionWidget(
            @PathVariable Integer id,
            @Valid @RequestBody PageWidgetPowerDistributionUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.updatePowerDistributionWidget(id, request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "페이지 위젯 수정")
    public ResponseEntity<ApiResponse<PageWidgetResponse>> updateWidget(
            @Parameter(description = "위젯 ID") @PathVariable Integer id,
            @Valid @RequestBody PageWidgetUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.updateWidget(id, request)));
    }

    @PatchMapping("/{id}/enabled")
    @Operation(summary = "위젯 UI 표시 on/off", description = "페이지에 위젯을 보여줄지 여부만 변경합니다.")
    public ResponseEntity<ApiResponse<PageWidgetResponse>> setEnabled(
            @Parameter(description = "위젯 ID") @PathVariable Integer id,
            @Valid @RequestBody PageWidgetEnabledRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.setEnabled(id, request)));
    }

    @PutMapping("/{id}/layout")
    @Operation(summary = "위젯 2D 배치 저장", description = "드래그 후 grid 좌표·크기만 저장합니다.")
    public ResponseEntity<ApiResponse<PageWidgetResponse>> replaceLayout(
            @Parameter(description = "위젯 ID") @PathVariable Integer id,
            @Valid @RequestBody PageWidgetLayoutRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.replaceLayout(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "페이지 위젯 삭제")
    public ResponseEntity<ApiResponse<Integer>> deleteWidget(
            @Parameter(description = "위젯 ID") @PathVariable Integer id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pageWidgetQueryService.deleteWidget(id)));
    }
}
