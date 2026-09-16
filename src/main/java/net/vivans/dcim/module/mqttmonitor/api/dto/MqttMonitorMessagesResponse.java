package net.vivans.dcim.module.mqttmonitor.api.dto;

import java.util.List;

public record MqttMonitorMessagesResponse(
        MqttMonitorSessionResponse session,
        List<MqttMonitorMessageResponse> messages
) {
}
