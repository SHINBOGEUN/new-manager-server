package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.DeviceRackPlacement;
import net.vivans.dcim.module.device.domain.model.DeviceMountType;
import net.vivans.dcim.module.device.domain.model.DeviceRackSide;

public record DeviceRackPlacementResponse(
        Integer id, Integer deviceId, DeviceMountType mountType, String rackLocationCode, String rackLocationName,
        DeviceRackSide rackSide, Integer uPosition, Integer uHeight, Integer lastU, String formFactor
) {
    public static DeviceRackPlacementResponse from(DeviceRackPlacement placement) {
        return new DeviceRackPlacementResponse(placement.getId(), placement.getDevice().getId(), placement.getMountType(),
                placement.getRackLocation() == null ? null : placement.getRackLocation().getCode(),
                placement.getRackLocation() == null ? null : placement.getRackLocation().getName(), placement.getRackSide(),
                placement.getUPosition(), placement.getUHeight(), placement.lastU(), placement.getFormFactor());
    }
}
