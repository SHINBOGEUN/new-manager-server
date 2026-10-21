package net.vivans.dcim.module.alarm.api;

import java.math.BigDecimal;
import java.time.Instant;

public record ThresholdRuleResponse(Long id, Integer deviceId, String deviceName, String modelName,
                                    String protocol, String pointName, String dataPointType,
                                    String unit, String operator, BigDecimal thresholdValue,
                                    String severity, boolean enabled, Instant createdDt, Instant updatedDt) {}
