package net.vivans.dcim.module.collectortask.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskCreateRequest;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskGroupRequest;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskGroupResponse;
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
@Tag(name = "collection-task", description = "모델별 수집 Task / 주기 그룹 관리 API")
public class CollectionTaskController {

    private final CollectionTaskService collectionTaskService;

    @GetMapping
    @Operation(summary = "수집 task 목록 조회")
    public ResponseEntity<ApiResponse<List<CollectionTaskResponse>>> getTasks(
            @Parameter(description = "모델 ID") @RequestParam(required = false) Integer modelId,
            @Parameter(description = "스크립트 타입 common_code ID") @RequestParam(required = false) Integer scriptTypeId,
            @Parameter(description = "활성 여부") @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.getTasks(modelId, scriptTypeId, active)));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "수집 task 단건 조회")
    public ResponseEntity<ApiResponse<CollectionTaskResponse>> getTask(
            @Parameter(description = "Task ID") @PathVariable Integer taskId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.getTask(taskId)));
    }

    @PostMapping
    @Operation(summary = "수집 task 생성 (모델 1개 + 주기 그룹)")
    public ResponseEntity<ApiResponse<CollectionTaskResponse>> createTask(
            @Valid @RequestBody CollectionTaskCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.createTask(request)));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "수집 task 메타 수정")
    public ResponseEntity<ApiResponse<CollectionTaskResponse>> updateTask(
            @Parameter(description = "Task ID") @PathVariable Integer taskId,
            @Valid @RequestBody CollectionTaskUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.updateTask(taskId, request)));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "수집 task 삭제")
    public ResponseEntity<ApiResponse<Integer>> deleteTask(
            @Parameter(description = "Task ID") @PathVariable Integer taskId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.deleteTask(taskId)));
    }

    @PatchMapping("/{taskId}/toggle")
    @Operation(summary = "수집 task 활성/비활성 전환")
    public ResponseEntity<ApiResponse<CollectionTaskResponse>> toggleTask(
            @Parameter(description = "Task ID") @PathVariable Integer taskId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.toggleTask(taskId)));
    }

    @PostMapping("/{taskId}/groups")
    @Operation(summary = "주기 그룹 추가")
    public ResponseEntity<ApiResponse<CollectionTaskGroupResponse>> createGroup(
            @Parameter(description = "Task ID") @PathVariable Integer taskId,
            @Valid @RequestBody CollectionTaskGroupRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.createGroup(taskId, request)));
    }

    @PutMapping("/{taskId}/groups/{groupId}")
    @Operation(summary = "주기 그룹 수정 (cron, 장비 목록)")
    public ResponseEntity<ApiResponse<CollectionTaskGroupResponse>> updateGroup(
            @Parameter(description = "Task ID") @PathVariable Integer taskId,
            @Parameter(description = "그룹 ID") @PathVariable Integer groupId,
            @Valid @RequestBody CollectionTaskGroupRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.updateGroup(taskId, groupId, request)));
    }

    @DeleteMapping("/{taskId}/groups/{groupId}")
    @Operation(summary = "주기 그룹 삭제")
    public ResponseEntity<ApiResponse<Integer>> deleteGroup(
            @Parameter(description = "Task ID") @PathVariable Integer taskId,
            @Parameter(description = "그룹 ID") @PathVariable Integer groupId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.deleteGroup(taskId, groupId)));
    }

    @PatchMapping("/{taskId}/groups/{groupId}/toggle")
    @Operation(summary = "주기 그룹 활성/비활성 전환")
    public ResponseEntity<ApiResponse<CollectionTaskGroupResponse>> toggleGroup(
            @Parameter(description = "Task ID") @PathVariable Integer taskId,
            @Parameter(description = "그룹 ID") @PathVariable Integer groupId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionTaskService.toggleGroup(taskId, groupId)));
    }
}
