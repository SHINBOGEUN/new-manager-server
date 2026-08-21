package net.vivans.dcim.module.live.api.dto;

import java.util.List;

public record LiveDeviceResponse(
        Integer deviceId,
        String deviceName,
        String locationNodeName,
        Integer modelId,
        String modelName,
        List<LivePointResponse> points
) {
}
