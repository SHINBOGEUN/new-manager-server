package net.vivans.dcim.module.device.api.dto;

public record DeviceMonitoringPolicyResponse(
        Integer deviceId,
        String modeCode,
        String modeName,
        boolean explicitlyConfigured
) {}
