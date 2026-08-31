package net.vivans.dcim.module.device.api.dto;

public record DeviceCapabilityPointResponse(
        Integer pointId,
        String name,
        String unit,
        Double scale,
        String oidTemplate,
        String resolvedOid,
        boolean requiresInstance
) {
}
