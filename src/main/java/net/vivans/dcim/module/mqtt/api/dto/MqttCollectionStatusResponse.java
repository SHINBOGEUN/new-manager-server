package net.vivans.dcim.module.mqtt.api.dto;

import java.util.List;

public record MqttCollectionStatusResponse(
        MqttCollectionStatusSummary summary,
        MqttCollectionSourceSummary sourceSummary,
        List<MqttCollectionStatusRow> rows
) {
}
