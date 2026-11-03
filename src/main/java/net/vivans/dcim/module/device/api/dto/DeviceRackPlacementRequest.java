package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.DeviceMountType;
import net.vivans.dcim.module.device.domain.model.DeviceRackSide;

public record DeviceRackPlacementRequest(
        DeviceMountType mountType,
        String rackLocationCode,
        DeviceRackSide rackSide,
        Integer uPosition,
        Integer uHeight,
        String formFactor,
        Double positionX,
        Double positionY,
        Double positionZ,
        Boolean clearPosition,
        String sceneModelKey,
        Boolean updateSceneModel,
        Double visualLengthRatio
) {
    public DeviceRackPlacementRequest(DeviceMountType mountType, String rackLocationCode, DeviceRackSide rackSide, Integer uPosition, Integer uHeight, String formFactor, Double positionX, Double positionY, Double positionZ, Boolean clearPosition, String sceneModelKey, Boolean updateSceneModel) {
        this(mountType, rackLocationCode, rackSide, uPosition, uHeight, formFactor, positionX, positionY, positionZ, clearPosition, sceneModelKey, updateSceneModel, null);
    }
}
