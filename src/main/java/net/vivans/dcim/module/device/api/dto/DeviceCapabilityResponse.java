package net.vivans.dcim.module.device.api.dto;

import java.util.List;

public record DeviceCapabilityResponse(
        Integer deviceId,
        String deviceName,
        String locationNodeName,
        Integer modelId,
        String modelName,
        String manufacturer,
        DeviceCapabilityEndpointResponse endpoint,
        List<DeviceCapabilityPointResponse> points
) {
}
