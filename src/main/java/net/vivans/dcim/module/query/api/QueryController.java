package net.vivans.dcim.module.query.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.query.api.dto.ChartWidgetResponse;
import net.vivans.dcim.module.query.api.dto.CountWidgetResponse;
import net.vivans.dcim.module.query.api.dto.LastWidgetResponse;
import net.vivans.dcim.module.query.application.ChartQueryService;
import net.vivans.dcim.module.query.application.CountQueryService;
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
    private final CountQueryService countQueryService;
    private final ChartQueryService chartQueryService;

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

    @GetMapping("/count")
    @Operation(
            summary = "위젯 장비 수 조회",
            description = "widgetId의 page_widget에 묶인 enabled 장비 수를 셉니다. "
                    + "queryKind=count 만 허용. 전체 count와 model별 byModel을 반환합니다. "
                    + "Influx를 쓰지 않습니다."
    )
    public ResponseEntity<ApiResponse<CountWidgetResponse>> getCount(
            @Parameter(description = "page_widget id", example = "12", required = true)
            @RequestParam Integer widgetId,
            @Parameter(description = "집계 방식 override: total | by_model | model (미지정 시 위젯 설정)")
            @RequestParam(required = false) String countMode,
            @Parameter(description = "countMode=model 일 때 modelId (미지정 시 위젯 설정)")
            @RequestParam(required = false) Integer countModelId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                countQueryService.getCount(widgetId, countMode, countModelId)));
    }

    @GetMapping("/chart")
    @Operation(
            summary = "위젯 시계열 차트 조회",
            description = "queryKind=chart 위젯의 장비/모델 범위 + pointNames로 Influx 시계열을 조회합니다. "
                    + "seriesMode: per_device | sum | by_phase(point L1/L2/L3) | by_path(location_node)."
    )
    public ResponseEntity<ApiResponse<ChartWidgetResponse>> getChart(
            @Parameter(description = "page_widget id", example = "12", required = true)
            @RequestParam Integer widgetId,
            @Parameter(description = "기간 preset override")
            @RequestParam(required = false) String rangePreset,
            @Parameter(description = "aggregateWindow override: 1m|5m|15m|1h|1d")
            @RequestParam(required = false) String window,
            @Parameter(description = "seriesMode override: per_device(PDU) | sum(Total) | by_phase | by_path")
            @RequestParam(required = false) String seriesMode
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                chartQueryService.getChart(widgetId, rangePreset, window, seriesMode)));
    }
}
