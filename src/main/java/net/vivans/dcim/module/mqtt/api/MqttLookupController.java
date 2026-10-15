package net.vivans.dcim.module.mqtt.api;

import java.util.List;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import net.vivans.dcim.module.mqtt.api.dto.*;
import net.vivans.dcim.module.mqtt.application.MqttLookupService;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 기존 서비스 API Key 인증을 사용하는 Sensor Data용 설정 조회 API. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/mqtt")
@Tag(name = "mqtt-lookup", description = "GPU·LoRa 등 공통 MQTT 수집 설정 조회")
public class MqttLookupController {
    private final MqttLookupService service;

    @GetMapping("/identities/resolve")
    @Operation(summary = "외부 식별값으로 활성 MQTT 장비 조회")
    public ResponseEntity<ApiResponse<MqttIdentityLookupResponse>> resolve(
            @RequestParam MqttIdentityType identityType, @RequestParam String externalId) {
        return ResponseEntity.ok(ApiResponse.ok(service.resolve(identityType, externalId).orElse(null)));
    }

    @GetMapping("/identities/bulk")
    @Operation(summary = "활성 MQTT 장비 식별값 일괄 조회")
    public ResponseEntity<ApiResponse<List<MqttIdentityLookupResponse>>> identities() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllEnabledIdentities()));
    }

    @GetMapping("/mappings/models/bulk")
    @Operation(summary = "모델별 활성 MQTT 포인트 설정 일괄 조회")
    public ResponseEntity<ApiResponse<List<DeviceModelMqttPointResponse>>> mappings() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllEnabledModelMappings()));
    }
}
