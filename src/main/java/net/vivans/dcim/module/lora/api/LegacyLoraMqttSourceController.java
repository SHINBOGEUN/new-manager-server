package net.vivans.dcim.module.lora.api;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.api.dto.*;
import net.vivans.dcim.module.mqtt.application.MqttSourceService;
import net.vivans.dcim.module.mqtt.domain.model.MqttSourceType;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 전환 전 Sensor Data 호환용. AGENT를 기존 LoRa 파서에 전달하지 않는다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/lora/sources")
public class LegacyLoraMqttSourceController {
    private final MqttSourceService service;

    @GetMapping("/bulk")
    public ResponseEntity<ApiResponse<List<MqttSourceResponse>>> bulk() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllForSensorData().stream()
                .filter(source -> source.sourceType() == MqttSourceType.CHIRPSTACK_LORA).toList()));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MqttSourceStatusResponse>> status(@PathVariable Integer id,
            @RequestBody MqttSourceStatusReportRequest request) {
        service.requireLoraSource(id);
        return ResponseEntity.ok(ApiResponse.ok(service.reportStatus(id, request)));
    }
}
