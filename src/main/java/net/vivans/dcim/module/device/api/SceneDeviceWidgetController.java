package net.vivans.dcim.module.device.api;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.application.RackWidgetService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/assets/scene-devices/{deviceId}")
public class SceneDeviceWidgetController {
    private final RackWidgetService service;
    @GetMapping("/widget-sources") public Object sources(@PathVariable int deviceId) { return ApiResponse.ok(service.equipmentSources(deviceId)); }
    @GetMapping("/widgets/devices") public Object devices(@PathVariable int deviceId) { return ApiResponse.ok(service.equipmentDevices(deviceId)); }
    @GetMapping("/widgets") public Object get(@PathVariable int deviceId) { return ApiResponse.ok(service.equipmentGet(deviceId)); }
    @PreAuthorize("hasRole('ADMIN')") @PutMapping("/widgets") public Object save(@PathVariable int deviceId, @RequestBody List<RackWidgetService.Widget> widgets) { return ApiResponse.ok(service.equipmentSave(deviceId, widgets)); }
    @GetMapping("/widgets/values") public Object values(@PathVariable int deviceId) { return ApiResponse.ok(service.equipmentValues(deviceId)); }
    @PreAuthorize("hasRole('ADMIN')") @PostMapping("/widgets/preview") public Object preview(@PathVariable int deviceId, @RequestBody List<RackWidgetService.Widget> widgets) { return ApiResponse.ok(service.equipmentPreview(deviceId, widgets)); }
}
