package net.vivans.dcim.module.alarm.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ThresholdRuleRequest(
        @NotNull @Positive Integer deviceId,
        @NotBlank @Size(max = 100) String protocol,
        @NotBlank @Size(max = 120) String pointName,
        @NotBlank @Pattern(regexp = "ABOVE|BELOW") String operator,
        @Digits(integer = 16, fraction = 8) BigDecimal warningThresholdValue,
        @Digits(integer = 16, fraction = 8) BigDecimal criticalThresholdValue,
        Boolean enabled
) {}
