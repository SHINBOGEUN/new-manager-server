package net.vivans.dcim.module.lora.api.dto;

import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceStatus;
import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceStatusType;

import java.time.Instant;

public record LoraMqttSourceStatusResponse(
        LoraMqttSourceStatusType status,
        Instant lastConnectedAt,
        Instant lastMessageAt,
        Instant lastStatusAt,
        long messageCount,
        long errorCount,
        long reconnectCount,
        String lastError
) {
    public static LoraMqttSourceStatusResponse from(LoraMqttSourceStatus status) {
        if (status == null) return new LoraMqttSourceStatusResponse(LoraMqttSourceStatusType.NOT_SYNCED, null, null, null, 0, 0, 0, null);
        return new LoraMqttSourceStatusResponse(status.getStatus(), status.getLastConnectedAt(), status.getLastMessageAt(),
                status.getLastStatusAt(), status.getMessageCount(), status.getErrorCount(), status.getReconnectCount(), status.getLastError());
    }
}
