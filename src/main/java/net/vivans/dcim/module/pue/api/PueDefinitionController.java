package net.vivans.dcim.module.pue.api;
import io.swagger.v3.oas.annotations.tags.Tag; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import net.vivans.dcim.module.pue.api.dto.*; import net.vivans.dcim.module.pue.application.PueDefinitionService; import net.vivans.dcim.shared.api.ApiResponse; import org.springframework.web.bind.annotation.*; import org.springframework.security.access.prepost.PreAuthorize; import java.util.List;
@RestController @RequiredArgsConstructor @Tag(name="pue-definition") @RequestMapping("/api/manager/pue-definitions")
public class PueDefinitionController { private final PueDefinitionService service;
 @GetMapping public ApiResponse<List<PueDefinitionResponse>> list(){return ApiResponse.ok(service.list());}
 @PreAuthorize("hasRole('ADMIN')") @PostMapping public ApiResponse<PueDefinitionResponse> create(@Valid @RequestBody PueDefinitionRequest r){return ApiResponse.ok(service.create(r));}
 @PreAuthorize("hasRole('ADMIN')") @PutMapping("/{id}") public ApiResponse<PueDefinitionResponse> update(@PathVariable Integer id,@Valid @RequestBody PueDefinitionRequest r){return ApiResponse.ok(service.update(id,r));}
 @PreAuthorize("hasRole('ADMIN')") @PatchMapping("/{id}/collection-enabled") public ApiResponse<PueDefinitionResponse> toggle(@PathVariable Integer id,@RequestParam boolean enabled){return ApiResponse.ok(service.setCollectionEnabled(id,enabled));}
 @PreAuthorize("hasRole('ADMIN')") @DeleteMapping("/{id}") public ApiResponse<Integer> delete(@PathVariable Integer id){service.delete(id);return ApiResponse.ok(id);}
}
