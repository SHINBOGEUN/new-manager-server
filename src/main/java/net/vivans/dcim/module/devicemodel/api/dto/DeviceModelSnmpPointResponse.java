package net.vivans.dcim.module.devicemodel.api.dto;

import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;

public record DeviceModelSnmpPointResponse(
        Integer id,
        Integer modelId,
        Integer protocolId,
        String name,
        String oid,
        boolean requiresInstance,
        String unit,
        Double scale,
        boolean enabled
) {

    public static DeviceModelSnmpPointResponse from(DeviceModelSnmpPoint point) {
        return new DeviceModelSnmpPointResponse(
                point.getId(),
                point.getModelProtocol().getDeviceModel().getId(),
                point.getModelProtocol().getId(),
                point.getName(),
                point.getOid(),
                point.isRequiresInstance(),
                point.getUnit(),
                point.getScale(),
                point.isEnabled()
        );
    }
}
