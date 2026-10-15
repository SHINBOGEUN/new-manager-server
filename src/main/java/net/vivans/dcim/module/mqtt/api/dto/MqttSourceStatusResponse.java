package net.vivans.dcim.module.mqtt.api.dto;

import net.vivans.dcim.module.mqtt.domain.model.MqttSourceStatus;
import net.vivans.dcim.module.mqtt.domain.model.MqttSourceStatusType;

import java.time.Instant;

public record MqttSourceStatusResponse(
        MqttSourceStatusType status,
        Instant lastConnectedAt,
        Instant lastMessageAt,
        Instant lastStatusAt,
        long messageCount,
        long errorCount,
        long reconnectCount,
        String lastError
) {
    public static MqttSourceStatusResponse from(MqttSourceStatus status) {
        if (status == null) return new MqttSourceStatusResponse(MqttSourceStatusType.NOT_SYNCED, null, null, null, 0, 0, 0, null);
        return new MqttSourceStatusResponse(status.getStatus(), status.getLastConnectedAt(), status.getLastMessageAt(),
                status.getLastStatusAt(), status.getMessageCount(), status.getErrorCount(), status.getReconnectCount(), status.getLastError());
    }
}
