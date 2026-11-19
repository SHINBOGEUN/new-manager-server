package net.vivans.dcim.module.collectortask.api;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionEligibilityService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class CollectionWritePermissionController {
    private final CollectionEligibilityService service;
    @PostMapping("/api/internal/collection/write-permission")
    public ApiResponse<CollectionEligibilityService.WritePermission> authorize(@RequestBody CollectionEligibilityService.WriteRequest request){return ApiResponse.ok(service.authorize(request));}
}
