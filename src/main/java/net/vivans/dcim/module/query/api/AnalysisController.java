package net.vivans.dcim.module.query.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.query.api.dto.AnalysisDeviceResponse;
import net.vivans.dcim.module.query.api.dto.AnalysisMeasurementsRequest;
import net.vivans.dcim.module.query.application.AnalysisQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/analysis")
@Tag(name = "analysis", description = "분석 페이지 과거 측정값 조회 API")
public class AnalysisController {

    private final AnalysisQueryService queryService;

    @PostMapping("/components")
    public ResponseEntity<ApiResponse<List<AnalysisQueryService.ComponentOption>>> getComponents(
            @Valid @RequestBody AnalysisMeasurementsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getComponents(request)));
    }

    @PostMapping("/measurements")
    @Operation(
            summary = "선택 장비·포인트의 과거 측정값 조회",
            description = "ANALYSIS 페이지에 등록되고 노출 허용된 포인트만 조회합니다. "
                    + "기간에 따라 raw 또는 30m/3h/12h/1d 통계를 반환합니다."
    )
    public ResponseEntity<ApiResponse<List<AnalysisDeviceResponse>>> getMeasurements(
            @Valid @RequestBody AnalysisMeasurementsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getMeasurements(request)));
    }
}
