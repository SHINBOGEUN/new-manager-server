package net.vivans.dcim.module.operations.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.operations.api.dto.CollectionOperationsHealthResponse;
import net.vivans.dcim.module.operations.api.dto.CollectionReconciliationResponse;
import net.vivans.dcim.module.operations.application.CollectionOperationsHealthService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/operations/collection")
@Tag(name = "collection-operations", description = "수집 운영 상태와 Collector 동기화 API")
public class CollectionOperationsController {

    private final CollectionOperationsHealthService healthService;

    @GetMapping("/health")
    @Operation(summary = "수집 운영 연결 상태 조회")
    public ResponseEntity<ApiResponse<CollectionOperationsHealthResponse>> getHealth() {
        return ResponseEntity.ok(ApiResponse.ok(healthService.getHealth()));
    }

    @PostMapping("/reconcile")
    @Operation(summary = "활성 수집 작업을 Collector에 다시 동기화")
    public ResponseEntity<ApiResponse<CollectionReconciliationResponse>> reconcile() {
        return ResponseEntity.ok(ApiResponse.ok(healthService.reconcile()));
    }
}
