package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceRegistrationOptionsResponse;
import net.vivans.dcim.module.device.api.dto.DeviceRegistrationRequest;
import net.vivans.dcim.module.device.api.dto.DeviceRegistrationResponse;
import net.vivans.dcim.module.device.application.DeviceRegistrationService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/device-registrations")
@Tag(name = "device-registration", description = "장비 등록 마법사 API")
public class DeviceRegistrationController {
    private final DeviceRegistrationService registrationService;
    private final net.vivans.dcim.module.collectortask.application.DeviceCollectionSettingService collectionSettings;

    @GetMapping("/options")
    @Operation(summary = "모델별 등록 옵션 조회")
    public ResponseEntity<ApiResponse<DeviceRegistrationOptionsResponse>> options(@RequestParam Integer modelId) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.getOptions(modelId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "장비·통신 설정·수집 그룹 일괄 등록")
    public ResponseEntity<ApiResponse<DeviceRegistrationResponse>> register(
            @Valid @RequestBody DeviceRegistrationRequest request) {
        DeviceRegistrationResponse response=registrationService.register(request);
        return ResponseEntity.ok(ApiResponse.ok(new DeviceRegistrationResponse(response.device(),response.endpointId(),response.taskId(),response.groupId(),collectionSettings.getWithoutRuntime(response.device().id()))));
    }
}
