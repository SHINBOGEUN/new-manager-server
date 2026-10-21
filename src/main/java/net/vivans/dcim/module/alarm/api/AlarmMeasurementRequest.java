package net.vivans.dcim.module.alarm.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

public record AlarmMeasurementRequest(int deviceId, @NotBlank String protocol,
                                      @NotNull Instant measuredAt, @NotNull Map<String, Double> values) {}
