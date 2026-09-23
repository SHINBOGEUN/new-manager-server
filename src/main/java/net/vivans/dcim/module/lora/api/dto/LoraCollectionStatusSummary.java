package net.vivans.dcim.module.lora.api.dto;

import java.time.Instant;

public record LoraCollectionStatusSummary(
        int totalDeviceCount,
        int savedCount,
        int problemCount,
        boolean influxAvailable,
        Instant generatedAt
) {
}
