package net.vivans.dcim.module.alarm.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.alarm.application.ThresholdRuleService;
import net.vivans.dcim.module.device.application.DeviceMeasurementSourceCatalog;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/threshold-rules")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class ThresholdRuleController {
    private final ThresholdRuleService service;
    private final DeviceMeasurementSourceCatalog sources;

    @GetMapping public ApiResponse<List<ThresholdRuleResponse>> list() { return ApiResponse.ok(service.list()); }
    @GetMapping("/{id}") public ApiResponse<ThresholdRuleResponse> get(@PathVariable long id) { return ApiResponse.ok(service.get(id)); }
    @GetMapping("/sources") public ApiResponse<List<DeviceMeasurementSourceCatalog.Source>> sources() {
        return ApiResponse.ok(sources.availableSources().stream()
                .filter(source -> source.thresholdSupported() && source.unit() != null && !source.unit().isBlank())
                .toList());
    }
    @PostMapping public ApiResponse<ThresholdRuleResponse> create(@Valid @RequestBody ThresholdRuleRequest request) {
        return ApiResponse.ok(service.create(request));
    }
    @PutMapping("/{id}") public ApiResponse<ThresholdRuleResponse> update(@PathVariable long id,
            @Valid @RequestBody ThresholdRuleRequest request) { return ApiResponse.ok(service.update(id, request)); }
    @DeleteMapping("/{id}") public ApiResponse<ThresholdRuleResponse> disable(@PathVariable long id) {
        return ApiResponse.ok(service.disable(id));
    }
}
