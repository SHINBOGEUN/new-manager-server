package net.vivans.dcim.module.location.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LocationNodeUpdateRequest(
        @Schema(description = "위치 유형 ID (LOCATION_TYPE 그룹)", example = "1")
        @NotNull(message = "locationTypeId must not be null")
        Integer locationTypeId,

        @Schema(description = "노드 이름", example = "컨테이너 A (수정)")
        @NotBlank(message = "name must not be empty")
        String name,

        @Schema(description = "Rack 최대 U 용량 (Rack이 아닌 위치는 비워둠)", example = "42")
        Integer rackUCapacity
) {
    public LocationNodeUpdateRequest(Integer locationTypeId, String name) {
        this(locationTypeId, name, null);
    }
}
