package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PageWidgetCreateRequest(
        @Schema(description = "DEVICE_PAGE code", example = "dashboard")
        @NotBlank(message = "pageCode is required")
        String pageCode,

        @Schema(description = "위젯 표시명", example = "칠러")
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Schema(description = "사용 여부 (기본 true)", example = "true")
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

        @Schema(description = "aggregate 기간 preset: last_24h | today | yesterday | last_7d | this_month | last_month (usage 필수, 미지정 시 today)")
        String aggregateRangePreset,

        @Schema(description = "count만: total | by_model | model (미지정 시 조회 기본 by_model)")
        String countMode,

        @Schema(description = "countMode=model 일 때 필수 — device_model.id")
        Integer countModelId,

        @Schema(description = "chart만: devices | models (기본 devices)")
        String chartScope,

        @Schema(description = "chart만: per_device | sum | by_phase | by_path (기본 per_device)")
        String chartSeriesMode,

        @Schema(description = "chart만: last_24h | today | yesterday | last_7d | this_month")
        String chartRangePreset,

        @Schema(description = "chart만: 1m | 5m | 15m | 1h | 1d (기본 5m)")
        String chartWindow,

        @Schema(description = "last/aggregate 필수. pue면 총 전력 장비. chart+devices 필수. count/chart+models는 []", example = "[9]")
        List<Integer> deviceIds,

        @Schema(description = "aggregate pue만: IT 전력 장비", example = "[10,11]")
        List<Integer> itDeviceIds,

        @Schema(description = "chart+models 일 때 필수 — device_model.id 목록", example = "[10,20]")
        List<Integer> modelIds,

        @Schema(description = "last/aggregate/chart: pointNames (aggregate는 1개). count는 []", example = "[\"TOTAL_WT\"]")
        List<String> pointNames,

        @Schema(description = "2D 그리드 배치 (선택)")
        @Valid
        PageWidgetLayoutRequest layout
) {
}
