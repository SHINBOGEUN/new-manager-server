package net.vivans.dcim.module.device.api.dto;

public record DeviceCapabilityEndpointResponse(
        Integer endpointId,
        String host,
        int port,
        Integer instanceId
) {
}
