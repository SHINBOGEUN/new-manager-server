package net.vivans.dcim.module.mqtt.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.application.GpuAgentDiscoveryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
public class GpuAgentRegistrationController {
    private final GpuAgentDiscoveryService service;

    @PostMapping("/api/internal/gpu-agents/discover")
    public ApiResponse<Void> discover(@RequestBody Discovery request) {
        service.discover(request.uuid(), request.hostname(), request.metadata());
        return ApiResponse.ok();
    }

    @GetMapping("/api/manager/gpu-agents/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> pending() { return ApiResponse.ok(service.pending()); }

    @GetMapping("/api/manager/gpu-agents/device/{deviceId}")
    public ApiResponse<?> inventory(@PathVariable int deviceId) { return ApiResponse.ok(service.inventory(deviceId)); }

    @PostMapping("/api/manager/gpu-agents/{uuid}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> approve(@PathVariable String uuid) { return ApiResponse.ok(service.approve(uuid)); }

    @PostMapping("/api/manager/gpu-agents/{uuid}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> reject(@PathVariable String uuid) { service.reject(uuid); return ApiResponse.ok(); }

    public record Discovery(String uuid, String hostname, JsonNode metadata) {}
}

