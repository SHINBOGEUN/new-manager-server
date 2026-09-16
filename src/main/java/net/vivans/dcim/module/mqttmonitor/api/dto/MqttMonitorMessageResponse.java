package net.vivans.dcim.module.mqttmonitor.api.dto;

import java.time.Instant;

public record MqttMonitorMessageResponse(
        long id,
        Instant receivedAt,
        String topic,
        String payload,
        int qos,
        boolean retained
) {
}
