package net.vivans.dcim.module.device.api;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.application.SceneModelService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/scene-models")
public class SceneModelController {
    private final SceneModelService service;

    public record PanelInstallableRequest(@jakarta.validation.constraints.NotNull Boolean panelInstallable) {}

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{key}/panel-installable")
    public ResponseEntity<ApiResponse<SceneModelService.ModelResponse>> setPanelInstallable(@PathVariable String key, @jakarta.validation.Valid @RequestBody PanelInstallableRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.setPanelInstallable(key, request.panelInstallable())));
    }

    /** sceneRole: RACK, PANEL, FRAME 또는 null(일반 설비). */
    public record SceneRoleRequest(net.vivans.dcim.module.device.domain.model.SceneModelRole sceneRole) {}

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{key}/scene-role")
    public ResponseEntity<ApiResponse<SceneModelService.ModelResponse>> setSceneRole(@PathVariable String key, @RequestBody SceneRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.setSceneRole(key, request.sceneRole())));
    }

    public record DeviceLinkableRequest(@jakarta.validation.constraints.NotNull Boolean deviceLinkable) {}

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{key}/device-linkable")
    public ResponseEntity<ApiResponse<SceneModelService.ModelResponse>> setDeviceLinkable(@PathVariable String key, @jakarta.validation.Valid @RequestBody DeviceLinkableRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.setDeviceLinkable(key, request.deviceLinkable())));
    }

    public record RackInstallableRequest(@jakarta.validation.constraints.NotNull Boolean rackInstallable) {}

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{key}/rack-installable")
    public ResponseEntity<ApiResponse<SceneModelService.ModelResponse>> setRackInstallable(
            @PathVariable String key, @jakarta.validation.Valid @RequestBody RackInstallableRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.setRackInstallable(key, request.rackInstallable())));
    }

    public record RackMountOptionsRequest(@jakarta.validation.constraints.NotNull Boolean rackUInstallable, @jakarta.validation.constraints.NotNull Boolean rackAttachmentInstallable) {}

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{key}/rack-mount-options")
    public ResponseEntity<ApiResponse<SceneModelService.ModelResponse>> setRackMountOptions(@PathVariable String key, @jakarta.validation.Valid @RequestBody RackMountOptionsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.setRackMountOptions(key, request.rackUInstallable(), request.rackAttachmentInstallable())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SceneModelService.ModelResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.list()));
    }

    @GetMapping("/{key}/content")
    public ResponseEntity<Resource> content(@PathVariable String key) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("model/gltf-binary"))
                .header("Cache-Control", "private, max-age=300")
                .body(service.content(key));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SceneModelService.ModelResponse>> create(
            @RequestParam String name, @RequestParam(defaultValue = "기타") String category,
            @RequestParam double width, @RequestParam double height,
            @RequestParam double depth, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(service.create(name, category, width, height, depth, file)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{key}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SceneModelService.ModelResponse>> update(
            @PathVariable String key, @RequestParam String name, @RequestParam(required = false) String category,
            @RequestParam double width,
            @RequestParam double height, @RequestParam double depth, @RequestParam boolean enabled,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(key, name, category, width, height, depth, enabled, file)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable String key) {
        service.delete(key);
        return ResponseEntity.ok(ApiResponse.ok(key));
    }
}
