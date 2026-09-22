package net.vivans.dcim.module.location.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.location.api.dto.LocationNodeBulkCreateRequest;
import net.vivans.dcim.module.location.api.dto.LocationNodeCreateRequest;
import net.vivans.dcim.module.location.api.dto.LocationNodeParentUpdateRequest;
import net.vivans.dcim.module.location.api.dto.LocationNodeDeleteResponse;
import net.vivans.dcim.module.location.api.dto.LocationNodeResponse;
import net.vivans.dcim.module.location.api.dto.LocationNodeUpdateRequest;
import net.vivans.dcim.module.location.application.LocationNodeCommandService;
import net.vivans.dcim.module.location.application.LocationNodeQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/location-node")
@RequiredArgsConstructor
@Tag(name = "location-node", description = "위치 노드 관리 API")
public class LocationNodeController {

    private final LocationNodeQueryService nodeQueryService;
    private final LocationNodeCommandService nodeCommandService;

    @GetMapping
    @Operation(summary = "위치 노드 트리 조회 API", description = "전체 노드를 조회한 뒤 children에 중첩해 트리로 반환합니다.")
    public ResponseEntity<ApiResponse<List<LocationNodeResponse>>> getLocationNodes(
            @Parameter(description = "이름 부분 일치 검색") @RequestParam(required = false) String name,
            @Parameter(description = "직접 자식만 조회할 부모 code") @RequestParam(required = false) String parentCode,
            @Parameter(description = "위치 유형 ID") @RequestParam(required = false) Integer locationTypeId) {
        return ResponseEntity.ok(ApiResponse.ok(nodeQueryService.getLocationNodes(name, parentCode, locationTypeId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "위치 노드 등록 API", description = "code는 서버에서 10자 Base62 문자열로 자동 생성됩니다.")
    public ResponseEntity<ApiResponse<LocationNodeResponse>> createLocationNode(
            @Valid @RequestBody LocationNodeCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(nodeCommandService.createLocationNode(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bulk")
    @Operation(summary = "위치 노드 일괄 등록 API", description = "트리 구조 요청을 받아 부모부터 자식까지 순서대로 등록합니다.")
    public ResponseEntity<ApiResponse<List<LocationNodeResponse>>> createBatchLocationNodes(
            @Valid @RequestBody LocationNodeBulkCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(nodeCommandService.createBatchLocationNodes(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{code}")
    @Operation(summary = "위치 노드 수정 API", description = "locationType, name만 수정 가능합니다. code와 parent는 변경할 수 없습니다.")
    public ResponseEntity<ApiResponse<LocationNodeResponse>> updateLocationNode(
            @Parameter(description = "위치 노드 code") @PathVariable String code,
            @Valid @RequestBody LocationNodeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(nodeCommandService.updateLocationNode(code, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{code}/parent")
    @Operation(summary = "상위 노드 변경 API", description = "parentCode가 null이거나 비어 있으면 루트로 승격합니다.")
    public ResponseEntity<ApiResponse<LocationNodeResponse>> updateParentLocationNode(
            @Parameter(description = "위치 노드 code") @PathVariable String code,
            @Valid @RequestBody LocationNodeParentUpdateRequest request){
        return ResponseEntity.ok(ApiResponse.ok(nodeCommandService.updateParentLocationNode(code, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{code}")
    @Operation(summary = "위치 노드 삭제 API", description = "리프 노드만 삭제합니다. 참조 중인 장비는 UNASSIGNED로 이동합니다.")
    public ResponseEntity<ApiResponse<LocationNodeDeleteResponse>> deleteLocationNode(
            @Parameter(description = "위치 노드 code") @PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(nodeCommandService.deleteLocationNode(code)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{code}/subtree")
    @Operation(summary = "위치 노드 서브트리 삭제 API", description = "해당 노드와 모든 자손을 삭제합니다. 참조 중인 장비는 UNASSIGNED로 이동합니다.")
    public ResponseEntity<ApiResponse<LocationNodeDeleteResponse>> deleteLocationNodeSubtree(
            @Parameter(description = "위치 노드 code") @PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(nodeCommandService.deleteLocationNodeSubtree(code)));
    }
}
