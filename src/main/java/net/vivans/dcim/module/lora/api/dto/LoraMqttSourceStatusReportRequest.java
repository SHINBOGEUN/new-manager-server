package net.vivans.dcim.module.lora.api.dto;

import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceStatusType;

import java.time.Instant;

/** Sensor Data → Manager 상태 보고용. 카운터는 Sensor Data 기동 후 누적값이다. */
public record LoraMqttSourceStatusReportRequest(
        LoraMqttSourceStatusType status,
        Instant lastConnectedAt,
        Instant lastMessageAt,
        Long messageCount,
        Long errorCount,
        Long reconnectCount,
        String lastError,
        Instant reportedAt
) {
}
