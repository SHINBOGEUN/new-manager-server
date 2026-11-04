package net.vivans.dcim.module.device.api;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.application.RackWidgetService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/assets/racks/{code}/widgets")
public class RackWidgetController {
    private final RackWidgetService service;
    @GetMapping public ApiResponse<List<RackWidgetService.Widget>> get(@PathVariable String code) { return ApiResponse.ok(service.get(code)); }
    @GetMapping("/devices") public ApiResponse<List<RackWidgetService.RackWidgetDevice>> devices(@PathVariable String code) { return ApiResponse.ok(service.devices(code)); }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping public ApiResponse<List<RackWidgetService.Widget>> save(@PathVariable String code, @RequestBody List<RackWidgetService.Widget> widgets) { return ApiResponse.ok(service.save(code, widgets)); }
    @GetMapping("/values") public ApiResponse<List<List<RackWidgetService.WidgetValue>>> values(@PathVariable String code) { return ApiResponse.ok(service.values(code)); }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/preview") public ApiResponse<List<List<RackWidgetService.WidgetValue>>> preview(@PathVariable String code, @RequestBody List<RackWidgetService.Widget> widgets) { return ApiResponse.ok(service.preview(code, widgets)); }
}
