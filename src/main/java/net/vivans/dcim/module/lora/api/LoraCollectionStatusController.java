package net.vivans.dcim.module.lora.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.api.dto.LoraCollectionStatusResponse;
import net.vivans.dcim.module.lora.application.LoraCollectionStatusService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LoRa 실제 수집 검증(1차) 조회 API. Ops Console "LoRa 수집 상태" 탭 전용이며 조회만 제공한다(DB 변경 없음).
 * 미등록(devEUI/deviceName) 식별자는 운영 정책상 실패 이력으로 남기지 않으므로 이 API의 판정 대상이 아니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/lora/collection-status")
@Tag(name = "lora-collection-status", description = "LoRa 실제 수집 검증(장비별 상태) API")
public class LoraCollectionStatusController {

    private final LoraCollectionStatusService collectionStatusService;

    @GetMapping
    @Operation(summary = "LoRa 장비별 수집 상태 및 MQTT 소스 요약 조회")
    public ResponseEntity<ApiResponse<LoraCollectionStatusResponse>> getCollectionStatus() {
        return ResponseEntity.ok(ApiResponse.ok(collectionStatusService.getCollectionStatus()));
    }
}
