package net.vivans.dcim.module.collectortask.api;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import net.vivans.dcim.shared.api.ApiResponse;
import net.vivans.dcim.module.collectortask.application.DeviceCollectionSettingService;
import net.vivans.dcim.module.collectortask.api.dto.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/manager")
public class DeviceCollectionSettingController {
    private final DeviceCollectionSettingService service;
    @GetMapping("/collection-settings/options") public ApiResponse<DeviceCollectionSettingService.Options> options(){return ApiResponse.ok(service.options());}
    @PutMapping("/collection-settings/system") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DeviceCollectionSettingService.Options> system(@RequestBody DeviceCollectionSettingService.SystemRequest request){return ApiResponse.ok(service.updateSystem(request));}
    @GetMapping("/devices/{id}/collection-settings") public ApiResponse<List<DeviceCollectionSettingResponse>> get(@PathVariable Integer id){return ApiResponse.ok(service.get(id));}
    @PutMapping("/devices/{id}/collection-settings/{protocol}") @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ApiResponse<DeviceCollectionSettingResponse> save(@PathVariable Integer id,@PathVariable Integer protocol,@RequestBody DeviceCollectionSettingRequest request){return ApiResponse.ok(service.save(id,protocol,request));}
    @PostMapping("/collection-settings/bulk") @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ApiResponse<DeviceCollectionSettingService.BulkResponse> bulk(@RequestBody DeviceCollectionSettingService.BulkRequest request){return ApiResponse.ok(service.bulk(request));}
}
