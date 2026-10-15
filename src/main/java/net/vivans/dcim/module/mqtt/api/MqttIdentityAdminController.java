package net.vivans.dcim.module.mqtt.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.api.dto.MqttIdentityAdminRequest;
import net.vivans.dcim.module.mqtt.api.dto.MqttIdentityAdminResponse;
import net.vivans.dcim.module.mqtt.application.MqttIdentityAdminService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/mqtt/identities")
@Tag(name = "mqtt-identity", description = "MQTT/Dragino 외부 식별자(devEUI/deviceName) ↔ device 매칭 CRUD API")
public class MqttIdentityAdminController {

    private final MqttIdentityAdminService deviceLoraEndpointService;

    @GetMapping
    @Operation(summary = "MQTT 외부 식별자 목록 조회", description = "deviceId로 필터링할 수 있습니다.")
    public ResponseEntity<ApiResponse<List<MqttIdentityAdminResponse>>> getAll(
            @Parameter(description = "장비 ID (선택)") @RequestParam(required = false) Integer deviceId
    ) {
        List<MqttIdentityAdminResponse> result = deviceId == null
                ? deviceLoraEndpointService.getAll()
                : deviceLoraEndpointService.getAllByDeviceId(deviceId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "MQTT 외부 식별자 등록")
    public ResponseEntity<ApiResponse<MqttIdentityAdminResponse>> create(
            @Valid @RequestBody MqttIdentityAdminRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceLoraEndpointService.create(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "MQTT 외부 식별자 수정")
    public ResponseEntity<ApiResponse<MqttIdentityAdminResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody MqttIdentityAdminRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deviceLoraEndpointService.update(id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "MQTT 외부 식별자 삭제")
    public ResponseEntity<ApiResponse<Integer>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(deviceLoraEndpointService.delete(id)));
    }
}
