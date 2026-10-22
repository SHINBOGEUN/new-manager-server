package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.constraints.Pattern;

public record DeviceMonitoringPolicyRequest(
        @Pattern(regexp = "PERIODIC|ON_CHANGE") String modeCode
) {}
