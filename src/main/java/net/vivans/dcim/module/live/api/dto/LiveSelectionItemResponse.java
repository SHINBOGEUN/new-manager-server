package net.vivans.dcim.module.live.api.dto;

import java.util.List;

public record LiveSelectionItemResponse(
        Integer deviceId,
        List<String> pointNames
) {
}
