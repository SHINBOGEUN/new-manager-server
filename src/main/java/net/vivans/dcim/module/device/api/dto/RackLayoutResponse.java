package net.vivans.dcim.module.device.api.dto;

import java.util.List;
import net.vivans.dcim.module.device.domain.model.DeviceMountType;
import net.vivans.dcim.module.device.domain.model.DeviceRackSide;

public record RackLayoutResponse(
        String rackLocationCode,
        String rackLocationName,
        Integer rackUCapacity,
        List<RackLayoutItem> placements
) {
    public record RackLayoutItem(
            Integer deviceId, String deviceName, String assetCode, String modelName,
            DeviceMountType mountType, DeviceRackSide rackSide,
            Integer uPosition, Integer uHeight, Integer lastU, String formFactor, String assetColor,
            DeviceImageResponse primaryImage
    ) {}
}
