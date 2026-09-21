package net.vivans.dcim.module.lora.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointResponse;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraPointOverrideResponse;
import net.vivans.dcim.module.lora.api.dto.DeviceModelLoraPointResponse;
import net.vivans.dcim.module.lora.api.dto.LoraDeviceLookupResponse;
import net.vivans.dcim.module.lora.application.LoraLookupService;
import net.vivans.dcim.module.lora.domain.model.LoraIdType;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sensor Data 서버 전용 조회 API. sensor-data-service API Key로 호출하므로 ADMIN 권한을 요구하지 않는다
 * (기존 ManagerDeviceClient가 쓰는 /api/manager/devices/{id}와 동일한 인증 방식).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/lora")
@Tag(name = "lora-lookup", description = "Sensor Data가 TTL 캐시를 채우기 위해 쓰는 LoRa 설정 조회 API")
public class LoraLookupController {

    private final LoraLookupService loraLookupService;

    @GetMapping("/endpoints/resolve")
    @Operation(summary = "외부 식별자별 장비 조회 API", description = "캐시 미스 시 즉시 조회용. idType+externalId로 device를 찾는다.")
    public ResponseEntity<ApiResponse<LoraDeviceLookupResponse>> resolve(
            @Parameter(description = "DEV_EUI 또는 DEVICE_NAME") @RequestParam LoraIdType idType,
            @Parameter(description = "원본 식별자 값") @RequestParam String externalId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(loraLookupService.resolve(idType, externalId)));
    }

    @GetMapping("/endpoints/bulk")
    @Operation(summary = "활성 외부 식별자 전체 조회", description = "TTL 캐시 주기 갱신용 대량 조회.")
    public ResponseEntity<ApiResponse<List<DeviceLoraEndpointResponse>>> getAllEnabledEndpoints() {
        return ResponseEntity.ok(ApiResponse.ok(loraLookupService.getAllEnabledEndpoints()));
    }

    @GetMapping("/mappings/models/bulk")
    @Operation(summary = "모델별 매핑 일괄 조회 API", description = "활성화된 모델 단위 매핑 전체를 반환한다. TTL 캐시 주기 갱신용.")
    public ResponseEntity<ApiResponse<List<DeviceModelLoraPointResponse>>> getAllEnabledModelMappings() {
        return ResponseEntity.ok(ApiResponse.ok(loraLookupService.getAllEnabledModelMappings()));
    }

    @GetMapping("/mappings/overrides/bulk")
    @Operation(summary = "장비별 override 매핑 일괄 조회", description = "활성화된 장비 단위 override 전체를 반환한다. TTL 캐시 주기 갱신용.")
    public ResponseEntity<ApiResponse<List<DeviceLoraPointOverrideResponse>>> getAllEnabledOverrides() {
        return ResponseEntity.ok(ApiResponse.ok(loraLookupService.getAllEnabledOverrides()));
    }
}
