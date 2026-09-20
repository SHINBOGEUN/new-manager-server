package net.vivans.dcim.module.device.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceModbusReadingCreateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceModbusReadingResponse;
import net.vivans.dcim.module.device.api.dto.DeviceModbusReadingUpdateRequest;
import net.vivans.dcim.module.device.application.DeviceModbusReadingQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
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
@RequestMapping(
        "/api/manager/devices/{deviceId}/endpoints/{endpointId}/modbus/readings"
)
@Tag(
        name = "device-modbus-reading",
        description = "Modbus 회선별 수집 매핑 API"
)
public class DeviceModbusReadingController {

    private final DeviceModbusReadingQueryService readingQueryService;

    @GetMapping
    @Operation(summary = "Modbus reading 목록 조회 API",
            description = "소속 reading을 ID 오름차순으로 조회합니다. 비활성 항목도 포함하며, 없으면 빈 배열을 반환합니다.")
    public ResponseEntity<ApiResponse<List<DeviceModbusReadingResponse>>> getReadings(
            @Parameter(description = "수집 원본 장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "수집 원본 엔드포인트 ID") @PathVariable Integer endpointId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(readingQueryService.getReadings(deviceId, endpointId)));
    }

    @GetMapping("/{readingId}")
    @Operation(summary = "Modbus reading 단건 조회 API",
            description = "해당 endpoint 소속 reading을 조회합니다. 없거나 다른 endpoint 소속이면 404.")
    public ResponseEntity<ApiResponse<DeviceModbusReadingResponse>> getReading(
            @Parameter(description = "수집 원본 장비 ID") @PathVariable Integer deviceId,
            @Parameter(description = "수집 원본 엔드포인트 ID") @PathVariable Integer endpointId,
            @Parameter(description = "device_modbus_reading.id") @PathVariable Integer readingId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(readingQueryService.getReading(deviceId, endpointId, readingId)));
    }

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

    @PutMapping("/{readingId}")
    @Operation(
            summary = "Modbus reading 수정 API",
            description = "모델 point·Unit ID·주소·결과 장비·필드명·사용 여부를 "
                    + "전체 교체합니다. 모든 요청 필드(enabled 포함)는 필수이며, 소속 endpoint는 유지됩니다."
    )
    public ResponseEntity<ApiResponse<DeviceModbusReadingResponse>> updateReading(
            @Parameter(description = "수집 원본 장비 ID")
            @PathVariable Integer deviceId,

            @Parameter(description = "수집 원본 엔드포인트 ID")
            @PathVariable Integer endpointId,

            @Parameter(description = "device_modbus_reading.id")
            @PathVariable Integer readingId,

            @Valid @RequestBody DeviceModbusReadingUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                readingQueryService.updateReading(
                        deviceId,
                        endpointId,
                        readingId,
                        request
                )
        ));
    }

    @DeleteMapping("/{readingId}")
    @Operation(
            summary = "Modbus reading 삭제 API",
            description = "회선 매핑 한 건을 삭제합니다. 없으면 404. endpoint가 Modbus가 아니면 400."
    )
    public ResponseEntity<ApiResponse<Integer>> deleteReading(
            @Parameter(description = "수집 원본 장비 ID")
            @PathVariable Integer deviceId,

            @Parameter(description = "수집 원본 엔드포인트 ID")
            @PathVariable Integer endpointId,

            @Parameter(description = "device_modbus_reading.id")
            @PathVariable Integer readingId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                readingQueryService.deleteReading(deviceId, endpointId, readingId)
        ));
    }
}
