package net.vivans.dcim.module.collectortask.infrastructure.collector;
import java.time.Instant;
/** Collector 기술 사유는 내부에서만 보관하고 화면 DTO에는 전달하지 않는다. */
public record CollectorDeviceResultResponse(String collectorJobId,Integer taskId,Integer groupId,String protocol,Integer deviceId,
        int targetCount,int failedTargetCount,Instant lastAttemptAt,String lastOutcome,Instant lastSuccessAt,
        Instant lastFailureAt,String lastFailureCode,String lastFailureReason,int consecutiveFailures,
        boolean inFlight,Instant inFlightSince,Instant backoffUntil,int skippedBackoff,int skippedOverrun,
        int failureThreshold,long intervalSeconds,boolean jobEnabled) {}
