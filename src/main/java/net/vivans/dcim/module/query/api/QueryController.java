package net.vivans.dcim.module.query.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.query.api.dto.LastWidgetResponse;
import net.vivans.dcim.module.query.application.LastQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/query")
@Tag(name = "query", description = "측정값 조회 API")
public class QueryController {

    private final LastQueryService lastQueryService;

    @GetMapping("/last")
    @Operation(
            summary = "위젯 최신 측정값 조회",
            description = "widgetId의 page_widget에 묶인 device·pointNames로 Influx last 값을 조회합니다. "
                    + "queryKind=last 만 허용. 응답에 widgetName과 장비별 points를 포함합니다."
    )
    public ResponseEntity<ApiResponse<LastWidgetResponse>> getLast(
            @Parameter(description = "page_widget id", example = "12", required = true)
            @RequestParam Integer widgetId,
            @Parameter(description = "조회 범위(시간). 기본 24, 최대 168", example = "24")
            @RequestParam(required = false) Integer lookbackHours
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                lastQueryService.getLast(widgetId, lookbackHours)));
    }
}
