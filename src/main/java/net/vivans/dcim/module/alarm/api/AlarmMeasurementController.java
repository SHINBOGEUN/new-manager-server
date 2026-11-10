package net.vivans.dcim.module.alarm.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.alarm.application.ThresholdEvaluator;
import net.vivans.dcim.shared.api.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Collector(MQTT 수신)가 InfluxDB 저장 직후 보내는 측정값. 인증은 ApiKeyAuthFilter(MANAGER_API_KEY)가 담당한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/alarms/measurements")
public class AlarmMeasurementController {
    private final ThresholdEvaluator evaluator;

    @PostMapping
    public ApiResponse<Void> receive(@Valid @RequestBody AlarmMeasurementRequest request) {
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
