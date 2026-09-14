package net.vivans.dcim.module.device.api.dto;

import java.time.Instant;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistory;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistoryAction;

public record DeviceAssetHistoryResponse(
        Integer id, DeviceAssetHistoryAction action, Instant changedAt, String actorName, String reason,
        String previousAssetCode, String currentAssetCode,
        String previousSerialNumber, String currentSerialNumber,
        String previousStatusCode, String previousStatusName,
        String currentStatusCode, String currentStatusName,
        Boolean previousEnabled, Boolean currentEnabled
) {
    public static DeviceAssetHistoryResponse from(DeviceAssetHistory history) {
        return new DeviceAssetHistoryResponse(history.getId(), history.getAction(), history.getCreatedDt(),
                history.getActorName(), history.getReason(),
                history.getPreviousAssetCode(), history.getCurrentAssetCode(),
                history.getPreviousSerialNumber(), history.getCurrentSerialNumber(),
                history.getPreviousStatusCode(), history.getPreviousStatusName(),
                history.getCurrentStatusCode(), history.getCurrentStatusName(),
                history.getPreviousEnabled(), history.getCurrentEnabled());
    }
}
