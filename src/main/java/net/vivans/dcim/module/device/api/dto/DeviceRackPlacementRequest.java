package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.DeviceMountType;
import net.vivans.dcim.module.device.domain.model.DeviceRackSide;

public record DeviceRackPlacementRequest(
        DeviceMountType mountType,
        String rackLocationCode,
        DeviceRackSide rackSide,
        Integer uPosition,
        Integer uHeight,
        String formFactor
) {}
