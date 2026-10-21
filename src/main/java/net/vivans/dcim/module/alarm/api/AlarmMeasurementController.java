package net.vivans.dcim.module.alarm.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.alarm.application.ThresholdEvaluator;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/alarms/measurements")
public class AlarmMeasurementController {
    private final ThresholdEvaluator evaluator;

    @Value("${sensor-data.service.api-key:sensor-data-server}")
    private String sensorDataApiKey;

    @PostMapping
    public ApiResponse<Void> receive(@RequestHeader(value = "X-Api-Key", required = false) String key,
                                     @Valid @RequestBody AlarmMeasurementRequest request) {
        if (key == null || sensorDataApiKey == null || sensorDataApiKey.isBlank()
                || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),
                        sensorDataApiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (request.deviceId() < 1 || request.values().size() > 256) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid measurement batch");
        }
        request.values().forEach((point, value) -> {
            if (point != null && !point.isBlank() && value != null && Double.isFinite(value)) {
                evaluator.evaluateSample(request.deviceId(), request.protocol(), point, value, request.measuredAt());
            }
        });
        return ApiResponse.ok();
    }
}
