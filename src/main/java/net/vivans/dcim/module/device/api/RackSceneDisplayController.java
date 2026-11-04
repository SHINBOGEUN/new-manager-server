package net.vivans.dcim.module.device.api;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.application.RackSceneDisplayService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/assets/racks/{code}/scene-display")
public class RackSceneDisplayController {
    private final RackSceneDisplayService service;
    @GetMapping public ApiResponse<RackSceneDisplayService.Config> get(@PathVariable String code) { return ApiResponse.ok(service.get(code)); }
    @GetMapping("/values") public ApiResponse<RackSceneDisplayService.Display> values(@PathVariable String code) { return ApiResponse.ok(service.values(code)); }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping public ApiResponse<RackSceneDisplayService.Config> save(@PathVariable String code, @RequestBody RackSceneDisplayService.Config config) { return ApiResponse.ok(service.save(code,config)); }
}
