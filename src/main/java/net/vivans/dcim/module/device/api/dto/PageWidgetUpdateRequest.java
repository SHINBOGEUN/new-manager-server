package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PageWidgetUpdateRequest(
        @Schema(description = "위젯 표시명", example = "칠러")
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Schema(description = "사용 여부", example = "true")
        Boolean enabled,

        @Schema(description = "조회 종류: last | aggregate | count", example = "last")
        @NotBlank(message = "queryKind is required")
        String queryKind,

        @Schema(description = "aggregate 연산: delta_sum | weighted_avg | divide")
        String op,

        @Schema(description = "그룹: device | point | location")
        String groupBy,

        @Schema(description = "weighted_avg 가중치 포인트", example = "W")
        String weightPoint,

        @Schema(description = "divide 분자 포인트")
        String numeratorPoint,

        @Schema(description = "divide 분모 포인트")
        String denominatorPoint,

        @Schema(description = "조회 장비 ID", example = "[9]")
        @NotEmpty(message = "deviceIds is required")
        List<Integer> deviceIds,

        @Schema(description = "조회 포인트 이름")
        List<String> pointNames,

        @Schema(description = "2D 그리드 배치. null이면 기존 layout 유지")
        @Valid
        PageWidgetLayoutRequest layout
) {
}
