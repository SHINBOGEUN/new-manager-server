package net.vivans.dcim.module.mqttmonitor.api.dto;

import java.time.Instant;

public record MqttMonitorSessionResponse(
        String sessionId,
        String host,
        int port,
        String topic,
        String connectionState,
        String errorMessage,
        String webSocketTicket,
        Instant connectedAt,
        long latestMessageId,
        int bufferedMessageCount
) {
}
