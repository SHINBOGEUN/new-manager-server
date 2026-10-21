package net.vivans.dcim.module.alarm.api;

import java.math.BigDecimal;
import java.time.Instant;

public record AlarmEventResponse(Long id, String alarmType, String severity, String visibility,
                                 ThresholdDetails details, CollectionGapDetails collectionGapDetails,
                                 Instant raisedDt, Instant clearedDt) {
    public record ThresholdDetails(Long ruleId, Integer deviceId, String deviceName, String pointName,
                                   String unit, String operator, BigDecimal thresholdValue,
                                   BigDecimal measuredValue) {}
    public record CollectionGapDetails(Integer deviceId, String deviceName, String protocol) {}
}
