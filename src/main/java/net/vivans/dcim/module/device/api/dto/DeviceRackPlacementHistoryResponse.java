package net.vivans.dcim.module.device.api.dto;

import java.time.Instant;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistory;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistoryAction;
import net.vivans.dcim.module.device.domain.model.DeviceMountType;
import net.vivans.dcim.module.device.domain.model.DeviceRackSide;

public record DeviceRackPlacementHistoryResponse(
        Integer id, DeviceRackPlacementHistoryAction action, Instant changedAt,
        DeviceMountType previousMountType, String previousRackLocationCode, DeviceRackSide previousRackSide,
        Integer previousUPosition, Integer previousUHeight,
        DeviceMountType currentMountType, String currentRackLocationCode, DeviceRackSide currentRackSide,
        Integer currentUPosition, Integer currentUHeight
) {
    public static DeviceRackPlacementHistoryResponse from(DeviceRackPlacementHistory history) {
        return new DeviceRackPlacementHistoryResponse(history.getId(), history.getAction(), history.getCreatedDt(),
                history.getPreviousMountType(), history.getPreviousRackLocationCode(), history.getPreviousRackSide(),
                history.getPreviousUPosition(), history.getPreviousUHeight(),
                history.getCurrentMountType(), history.getCurrentRackLocationCode(), history.getCurrentRackSide(),
                history.getCurrentUPosition(), history.getCurrentUHeight());
    }
}
