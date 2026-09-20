package net.vivans.dcim.module.lora.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.api.dto.LoraIngestErrorLogCreateRequest;
import net.vivans.dcim.module.lora.api.dto.LoraIngestErrorLogResponse;
import net.vivans.dcim.module.lora.application.LoraIngestErrorLogService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/lora/error-logs")
@Tag(name = "lora-ingest-error-log", description = "LoRa 수집 미등록/미매핑/변환실패 오류 이력 API")
public class LoraIngestErrorLogController {

    private final LoraIngestErrorLogService loraIngestErrorLogService;

    @GetMapping
    @Operation(summary = "오류 이력 조회", description = "resolved로 필터링(생략 시 전체), 최신순 페이지네이션")
    public ResponseEntity<ApiResponse<Page<LoraIngestErrorLogResponse>>> getAll(
            @Parameter(description = "처리 여부 필터 (생략 시 전체)") @RequestParam(required = false) Boolean resolved,
            @Parameter(description = "0부터 시작하는 페이지") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                loraIngestErrorLogService.getAll(resolved, PageRequest.of(page, size))));
    }

    @Operation(
            summary = "오류 이력 적재 API",
            description = "Sensor Data 서버가 자체 RDB 없이 이 API를 통해 미등록/미매핑/변환실패 이력을 기록한다. "
                    + "sensor-data-service API Key로 호출하므로 ADMIN 권한을 요구하지 않는다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<LoraIngestErrorLogResponse>> create(
            @Valid @RequestBody LoraIngestErrorLogCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(loraIngestErrorLogService.create(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/resolve")
    @Operation(summary = "오류 이력 처리 완료 표시")
    public ResponseEntity<ApiResponse<LoraIngestErrorLogResponse>> resolve(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean resolved
    ) {
        return ResponseEntity.ok(ApiResponse.ok(loraIngestErrorLogService.resolve(id, resolved)));
    }
}
