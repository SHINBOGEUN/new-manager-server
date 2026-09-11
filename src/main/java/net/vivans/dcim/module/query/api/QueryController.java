package net.vivans.dcim.module.query.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.query.api.dto.AggregateWidgetResponse;
import net.vivans.dcim.module.query.api.dto.ChartWidgetResponse;
import net.vivans.dcim.module.query.api.dto.CountWidgetResponse;
import net.vivans.dcim.module.query.api.dto.LastWidgetResponse;
import net.vivans.dcim.module.query.api.dto.PueQueryRequest;
import net.vivans.dcim.module.query.api.dto.PueQueryResponse;
import net.vivans.dcim.module.query.api.dto.PsychrometricWidgetResponse;
import net.vivans.dcim.module.query.api.dto.PowerDistributionWidgetResponse;
import net.vivans.dcim.module.query.application.AggregateQueryService;
import net.vivans.dcim.module.query.application.ChartQueryService;
import net.vivans.dcim.module.query.application.CountQueryService;
import net.vivans.dcim.module.query.application.LastQueryService;
import net.vivans.dcim.module.query.application.PueQueryService;
import net.vivans.dcim.module.query.application.PsychrometricQueryService;
import net.vivans.dcim.module.query.application.PowerDistributionQueryService;
import net.vivans.dcim.module.query.application.WidgetTrendQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/query")
@Tag(name = "query", description = "측정값 조회 API")
public class QueryController {

    private final LastQueryService lastQueryService;
    private final CountQueryService countQueryService;
    private final ChartQueryService chartQueryService;
    private final AggregateQueryService aggregateQueryService;
    private final PueQueryService pueQueryService;
    private final PsychrometricQueryService psychrometricQueryService;
    private final PowerDistributionQueryService powerDistributionQueryService;
    private final WidgetTrendQueryService widgetTrendQueryService;

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
                    + "최대 두 단위를 허용하며, 두 단위일 때는 per_device 시리즈에 left/right 축 정보를 반환합니다. "
                    + "seriesMode: per_device | sum | by_phase(point L1/L2/L3) | by_path(location_node)."
    )
    public ResponseEntity<ApiResponse<ChartWidgetResponse>> getChart(
            @Parameter(description = "page_widget id", example = "12", required = true)
            @RequestParam Integer widgetId,
            @Parameter(description = "기간 preset override: last_24h|today|yesterday|last_3d|last_7d|this_month|last_month")
            @RequestParam(required = false) String rangePreset,
            @Parameter(description = "aggregateWindow override: 1m|5m|15m|1h|1d")
            @RequestParam(required = false) String window,
            @Parameter(description = "seriesMode override: per_device(PDU) | sum(Total) | by_phase | by_path")
            @RequestParam(required = false) String seriesMode
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                chartQueryService.getChart(widgetId, rangePreset, window, seriesMode)));
    }

    @GetMapping("/aggregate")
    @Operation(
            summary = "위젯 집계값 조회",
            description = "queryKind=aggregate 위젯의 preset(usage|power)으로 구간 집계값을 조회합니다. "
                    + "rangePreset 미지정 시 위젯 aggregateRangePreset, 없으면 usage→today / power→last_24h."
    )
    public ResponseEntity<ApiResponse<AggregateWidgetResponse>> getAggregate(
            @Parameter(description = "page_widget id", example = "12", required = true)
            @RequestParam Integer widgetId,
            @Parameter(description = "기간 preset: last_24h|today|yesterday|last_7d|this_month|last_month")
            @RequestParam(required = false) String rangePreset
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                aggregateQueryService.getAggregate(widgetId, rangePreset)));
    }

    @PostMapping("/pue")
    @Operation(
            summary = "PUE 계산",
            description = "위젯과 독립적으로 totalSources와 coolerSources의 장비별 POWER 포인트 최신값을 합산해 "
                    + "totalPower / coolerPower를 계산합니다."
    )
    public ResponseEntity<ApiResponse<PueQueryResponse>> getPue(
            @Valid @RequestBody PueQueryRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pueQueryService.getPue(request)));
    }

    @GetMapping("/pue")
    @Operation(summary = "저장된 PUE 위젯 조회", description = "rangePreset과 window를 함께 보내면 저장된 PUE 시계열 trend도 반환합니다.")
    public ResponseEntity<ApiResponse<PueQueryResponse>> getPue(
            @RequestParam Integer widgetId,
            @RequestParam(required = false) String rangePreset,
            @RequestParam(required = false) String window
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pueQueryService.getPue(widgetId, rangePreset, window)));
    }

    @GetMapping("/psychrometric")
    @Operation(
            summary = "사이코메트릭 위젯 조회",
            description = "선택한 온도·습도 소스의 InfluxDB 최근 5개 시간 평균과 최신 평균을 조회합니다. "
                    + "응답 data에는 TEMP_AVG(°C), HUM_AVG(%)를 같은 timeLabels 순서로 반환합니다."
    )
    public ResponseEntity<ApiResponse<PsychrometricWidgetResponse>> getPsychrometric(
            @RequestParam Integer widgetId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(psychrometricQueryService.getPsychrometric(widgetId)));
    }

    @GetMapping("/power-distribution")
    @Operation(summary = "전력 분배 위젯 조회", description = "W 단위 POWER 포인트의 최신값을 그룹별로 합산하고 비율을 반환합니다.")
    public ResponseEntity<ApiResponse<PowerDistributionWidgetResponse>> getPowerDistribution(
            @RequestParam Integer widgetId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(powerDistributionQueryService.getPowerDistribution(widgetId)));
    }

    @GetMapping("/widget-trend")
    @Operation(summary = "공통 위젯 트렌드 조회", description = "last·aggregate·psychrometric·power_distribution 위젯의 최근 시계열을 최대 두 단위로 반환합니다.")
    public ResponseEntity<ApiResponse<ChartWidgetResponse>> getWidgetTrend(
            @RequestParam Integer widgetId,
            @RequestParam(required = false, defaultValue = "last_3d") String rangePreset,
            @RequestParam(required = false, defaultValue = "15m") String window
    ) {
        return ResponseEntity.ok(ApiResponse.ok(widgetTrendQueryService.getTrend(widgetId, rangePreset, window)));
    }
}
