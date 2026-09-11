package net.vivans.dcim.module.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "최신값 위젯의 장비별 측정항목 선택")
public record PageWidgetLastSourceRequest(
        @NotNull(message = "deviceId is required")
        Integer deviceId,
        @NotEmpty(message = "pointNames is required")
        List<String> pointNames
) {
}
