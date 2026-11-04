package net.vivans.dcim.module.device.api;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.application.SceneBuiltinAssetService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/scene-builtin-assets")
public class SceneBuiltinAssetController {
    private final SceneBuiltinAssetService service;

    public record UpdateRequest(String label, Boolean enabled) {}

    @GetMapping
    public ResponseEntity<ApiResponse<List<SceneBuiltinAssetService.AssetResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.list()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<SceneBuiltinAssetService.AssetResponse>> update(
            @PathVariable String key, @RequestBody UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(key, request.label(), request.enabled())));
    }
}
