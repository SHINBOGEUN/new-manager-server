package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceModbusReadingCreateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceModbusReadingResponse;
import net.vivans.dcim.module.device.application.DeviceModbusReadingQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/modbus/readings"
)
@Tag(
        name = "device-modbus-reading",
        description = "Modbus 회선별 수집 매핑 API"
)
public class DeviceModbusReadingController {

    private final DeviceModbusReadingQueryService readingQueryService;

    @PostMapping
    @Operation(
            summary = "Modbus reading 등록 API",
            description = "회선의 Unit ID·주소와 결과를 저장할 장비·필드를 등록합니다."
    )
    public ResponseEntity<ApiResponse<DeviceModbusReadingResponse>> createReading(
            @Parameter(description = "수집 원본 장비 ID")
            @PathVariable Integer deviceId,

            @Parameter(description = "수집 원본 엔드포인트 ID")
            @PathVariable Integer endpointId,

            @Valid @RequestBody DeviceModbusReadingCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                readingQueryService.createReading(
                        deviceId,
                        endpointId,
                        request
                )
        ));
    }
}