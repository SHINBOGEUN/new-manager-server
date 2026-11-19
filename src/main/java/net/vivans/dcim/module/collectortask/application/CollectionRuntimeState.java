package net.vivans.dcim.module.collectortask.application;
import java.time.Instant;
public record CollectionRuntimeState(String state,int consecutiveFailures,int failureThreshold,Instant lastAttemptAt,
        Instant lastSuccessAt,Instant lastFailureAt,String reasonCode,String message,Instant backoffUntil,boolean inFlight) {}
