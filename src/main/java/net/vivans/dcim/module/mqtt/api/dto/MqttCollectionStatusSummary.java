package net.vivans.dcim.module.mqtt.api.dto;

import java.time.Instant;

public record MqttCollectionStatusSummary(
        int totalDeviceCount,
        int savedCount,
        int problemCount,
        boolean influxAvailable,
        Instant generatedAt
) {
}
