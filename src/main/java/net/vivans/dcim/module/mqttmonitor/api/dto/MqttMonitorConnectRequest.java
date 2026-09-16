package net.vivans.dcim.module.mqttmonitor.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MqttMonitorConnectRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9.-]{0,252}") String host,
        @Min(1) @Max(65535) int port,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._~:/#+-]{1,512}") String topic
) {
}
