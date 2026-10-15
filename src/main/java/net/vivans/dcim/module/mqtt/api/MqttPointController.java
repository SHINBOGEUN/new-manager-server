package net.vivans.dcim.module.mqtt.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.api.dto.MqttPointRequest;
import net.vivans.dcim.module.mqtt.api.dto.MqttPointResponse;
import net.vivans.dcim.module.mqtt.application.MqttPointService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/device-models/{modelId}/mqtt-points")
public class MqttPointController {
    private final MqttPointService service;
    @GetMapping
    public ApiResponse<List<MqttPointResponse>> list(@PathVariable Integer modelId) {
        return ApiResponse.ok(service.list(modelId));
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MqttPointResponse> create(@PathVariable Integer modelId, @Valid @RequestBody MqttPointRequest request) {
        return ApiResponse.ok(service.save(modelId, null, request));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MqttPointResponse> update(@PathVariable Integer modelId, @PathVariable Integer id, @Valid @RequestBody MqttPointRequest request) {
        return ApiResponse.ok(service.save(modelId, id, request));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> delete(@PathVariable Integer modelId, @PathVariable Integer id) {
        return ApiResponse.ok(service.delete(modelId, id));
    }
}
