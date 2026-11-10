package net.vivans.dcim.module.device.api;

import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartSeriesMode;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/widgets/options")
public class WidgetOptionsController {
    private static final Map<PageWidgetChartRangePreset, String> RANGE_LABELS = Map.of(
            PageWidgetChartRangePreset.last_1h, "최근 1시간",
            PageWidgetChartRangePreset.last_6h, "최근 6시간",
            PageWidgetChartRangePreset.last_24h, "최근 24시간",
            PageWidgetChartRangePreset.today, "오늘",
            PageWidgetChartRangePreset.yesterday, "어제",
            PageWidgetChartRangePreset.last_3d, "최근 3일",
            PageWidgetChartRangePreset.last_7d, "최근 7일",
            PageWidgetChartRangePreset.this_month, "이번 달",
            PageWidgetChartRangePreset.last_month, "지난 달"
    );
    private static final List<Option> WINDOWS = List.of(
            new Option("1m", "1분"), new Option("5m", "5분"), new Option("15m", "15분"),
            new Option("1h", "1시간"), new Option("1d", "1일")
    );
    private static final List<KindOption> KINDS = List.of(
            new KindOption("last", "현재값", "장비와 측정 항목의 최신값"),
            new KindOption("chart", "추이", "선택한 측정 항목의 시계열"),
            new KindOption("comparison", "비교", "상별 또는 위치별 묶음 비교"),
            new KindOption("aggregate", "집계·순위", "기간 합계, 순위, 최대 수요"),
            new KindOption("count", "개수", "사용 중인 장비 수"),
            new KindOption("calculated", "계산 지표", "PUE 등 계산된 지표"),
            new KindOption("psychrometric", "공기선도", "온도와 습도 기반 공기선도"),
            new KindOption("power_distribution", "전력 분배", "그룹별 전력 분배"),
            new KindOption("cctv", "CCTV", "카메라 영상")
    );

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Options> getOptions() {
        List<Option> ranges = List.of(PageWidgetChartRangePreset.values()).stream()
                .map(preset -> new Option(preset.name(), RANGE_LABELS.get(preset))).toList();
        List<String> seriesModes = List.of(PageWidgetChartSeriesMode.values()).stream()
                .map(Enum::name).toList();
        return ApiResponse.ok(new Options(ranges, WINDOWS, List.of("line", "bar", "area"), seriesModes, KINDS));
    }

    public record Option(String value, String label) {
    }

    public record KindOption(String value, String label, String description) {
    }

    public record Options(List<Option> rangePresets, List<Option> windows, List<String> chartStyles,
                          List<String> seriesModes, List<KindOption> kinds) {
    }
}
