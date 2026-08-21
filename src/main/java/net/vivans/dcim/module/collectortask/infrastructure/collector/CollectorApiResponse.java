package net.vivans.dcim.module.collectortask.infrastructure.collector;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CollectorApiResponse<T>(
        int status,
        String message,
        String error,
        Instant timestamp,
        T data
) {
}
