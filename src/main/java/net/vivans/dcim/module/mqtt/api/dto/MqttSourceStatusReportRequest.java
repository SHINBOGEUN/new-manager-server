package net.vivans.dcim.module.mqtt.api.dto;

import net.vivans.dcim.module.mqtt.domain.model.MqttSourceStatusType;

import java.time.Instant;

/** Collector(MQTT 수신) → Manager 상태 보고용. 카운터는 Collector(MQTT 수신) 기동 후 누적값이다. */
public record MqttSourceStatusReportRequest(
        MqttSourceStatusType status,
        Instant lastConnectedAt,
        Instant lastMessageAt,
        Long messageCount,
        Long errorCount,
        Long reconnectCount,
        String lastError,
        Instant reportedAt
) {
}
