package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PageWidgetUpdateRequest(
        @Schema(description = "위젯 표시명", example = "칠러")
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Schema(description = "사용 여부", example = "true")
        Boolean enabled,

        @Schema(description = "조회 종류: last | aggregate | count | chart", example = "last")
        @NotBlank(message = "queryKind is required")
        String queryKind,

        @Schema(description = "aggregate preset: usage | power | pue")
        String op,

        @Schema(description = "그룹: device | point | location")
        String groupBy,

        @Schema(description = "deprecated — ignored", deprecated = true)
        String weightPoint,

        @Schema(description = "deprecated — ignored", deprecated = true)
        String numeratorPoint,

        @Schema(description = "deprecated — ignored", deprecated = true)
        String denominatorPoint,

        @Schema(description = "aggregate 기간 preset (usage 필수, 미지정 시 today)")
        String aggregateRangePreset,

        @Schema(description = "count만: total | by_model | model")
        String countMode,

        @Schema(description = "countMode=model 일 때 필수 — device_model.id")
        Integer countModelId,

        @Schema(description = "chart만: devices | models")
        String chartScope,

        @Schema(description = "chart만: per_device | sum | by_phase | by_path")
        String chartSeriesMode,

        @Schema(description = "chart만: last_24h | today | yesterday | last_7d | this_month")
        String chartRangePreset,

        @Schema(description = "chart만: 1m | 5m | 15m | 1h | 1d")
        String chartWindow,

        @Schema(description = "last/aggregate 필수. pue면 총 전력 장비. chart+devices 필수. count/chart+models는 []")
        List<Integer> deviceIds,

        @Schema(description = "aggregate pue만: IT 전력 장비")
        List<Integer> itDeviceIds,

        @Schema(description = "chart+models 일 때 필수")
        List<Integer> modelIds,

        @Schema(description = "last/aggregate/chart: pointNames (aggregate는 1개). count는 []")
        List<String> pointNames,

        @Schema(description = "2D 그리드 배치. null이면 기존 layout 유지")
        @Valid
        PageWidgetLayoutRequest layout
) {
}
