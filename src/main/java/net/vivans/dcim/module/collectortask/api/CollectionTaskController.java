package net.vivans.dcim.module.collectortask.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskCreateRequest;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskResponse;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskUpdateRequest;
import net.vivans.dcim.module.collectortask.application.CollectionTaskService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/manager/collector/tasks")
@Tag(name = "collection-task", description = "수집 task 관리 API")
public class CollectionTaskController {

    private final CollectionTaskService collectionTaskService;

    @GetMapping
    @Operation(summary = "수집 task 목록 조회")
    public ResponseEntity<ApiResponse<List<CollectionTaskResponse>>> getTasks(
            @Parameter(description = "이름 부분 검색") @RequestParam(required = false) String name,
            @Parameter(description = "활성 여부") @RequestParam(required = false) Boolean active,
            @Parameter(description = "스크립트 타입 common_code ID (PROTOCOL_TYPE 그룹)") @RequestParam(required = false) Integer scriptTypeId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.getTasks(name, active, scriptTypeId)));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "수집 task 단건 조회")
    public ResponseEntity<ApiResponse<CollectionTaskResponse>> getTask(
            @Parameter(description = "Task ID (UUID)") @PathVariable String taskId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.getTask(taskId)));
    }

    @PostMapping
    @Operation(summary = "수집 task 생성")
    public ResponseEntity<ApiResponse<CollectionTaskResponse>> createTask(
            @Valid @RequestBody CollectionTaskCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.createTask(request)));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "수집 task 수정")
    public ResponseEntity<ApiResponse<CollectionTaskResponse>> updateTask(
            @Parameter(description = "Task ID (UUID)") @PathVariable String taskId,
            @Valid @RequestBody CollectionTaskUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.updateTask(taskId, request)));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "수집 task 삭제")
    public ResponseEntity<ApiResponse<String>> deleteTask(
            @Parameter(description = "Task ID (UUID)") @PathVariable String taskId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.deleteTask(taskId)));
    }

    @PatchMapping("/{taskId}/toggle")
    @Operation(summary = "수집 task 활성/비활성 전환")
    public ResponseEntity<ApiResponse<CollectionTaskResponse>> toggleTask(
            @Parameter(description = "Task ID (UUID)") @PathVariable String taskId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.toggleTask(taskId)));
    }
}
