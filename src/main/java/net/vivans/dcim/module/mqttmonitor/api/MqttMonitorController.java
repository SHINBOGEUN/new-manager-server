package net.vivans.dcim.module.mqttmonitor.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqttmonitor.api.dto.MqttMonitorConnectRequest;
import net.vivans.dcim.module.mqttmonitor.api.dto.MqttMonitorMessagesResponse;
import net.vivans.dcim.module.mqttmonitor.api.dto.MqttMonitorSessionResponse;
import net.vivans.dcim.module.mqttmonitor.application.MqttMonitorService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/mqtt-monitor/sessions")
@Tag(name = "mqtt-monitor", description = "Manager TCP MQTT 구독 모니터 API")
@PreAuthorize("hasRole('ADMIN')")
public class MqttMonitorController {

    private final MqttMonitorService mqttMonitorService;

    @PostMapping("/{sessionId}")
    @Operation(summary = "MQTT 모니터 연결 및 토픽 구독")
    public ResponseEntity<ApiResponse<MqttMonitorSessionResponse>> connect(
            @PathVariable String sessionId,
            @Valid @RequestBody MqttMonitorConnectRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(mqttMonitorService.connect(sessionId, request)));
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "MQTT 모니터 원문 메시지 조회")
    public ResponseEntity<ApiResponse<MqttMonitorMessagesResponse>> getMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") long afterId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(mqttMonitorService.getMessages(sessionId, afterId)));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "MQTT 모니터 구독 종료")
    public ResponseEntity<ApiResponse<Void>> disconnect(@PathVariable String sessionId) {
        mqttMonitorService.disconnect(sessionId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
