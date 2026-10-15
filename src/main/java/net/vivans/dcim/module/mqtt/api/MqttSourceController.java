package net.vivans.dcim.module.mqtt.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.api.dto.MqttSourceRequest;
import net.vivans.dcim.module.mqtt.api.dto.MqttSourceResponse;
import net.vivans.dcim.module.mqtt.api.dto.MqttSourceStatusReportRequest;
import net.vivans.dcim.module.mqtt.api.dto.MqttSourceStatusResponse;
import net.vivans.dcim.module.mqtt.application.MqttSourceService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/mqtt/sources")
@Tag(name = "mqtt-source", description = "LoRa·GPU·에이전트 MQTT 수집 소스 및 연결 상태 API")
public class MqttSourceController {
    private final MqttSourceService sourceService;

    @GetMapping
    @Operation(summary = "공통 MQTT 수집 소스/상태 목록 조회")
    public ResponseEntity<ApiResponse<List<MqttSourceResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(sourceService.getAll()));
    }

    /** Sensor Data API key 전용. 비활성 소스도 포함해 기존 client를 끊을 수 있게 한다. */
    @GetMapping("/bulk")
    @Operation(summary = "Sensor Data용 소스 일괄 조회")
    public ResponseEntity<ApiResponse<List<MqttSourceResponse>>> getAllForSensorData() {
        return ResponseEntity.ok(ApiResponse.ok(sourceService.getAllForSensorData()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<MqttSourceResponse>> create(@Valid @RequestBody MqttSourceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(sourceService.create(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MqttSourceResponse>> update(@PathVariable Integer id,
                                                                        @Valid @RequestBody MqttSourceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(sourceService.update(id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(sourceService.delete(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync")
    @Operation(summary = "Sensor Data에 즉시 소스 재동기화 요청")
    public ResponseEntity<ApiResponse<Void>> sync() {
        sourceService.requestImmediateSync();
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** Sensor Data API key 전용 상태 보고. */
    @PostMapping("/{id}/status")
    @Operation(summary = "Sensor Data 연결/수신 상태 보고")
    public ResponseEntity<ApiResponse<MqttSourceStatusResponse>> reportStatus(
            @PathVariable Integer id, @RequestBody MqttSourceStatusReportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(sourceService.reportStatus(id, request)));
    }
}
