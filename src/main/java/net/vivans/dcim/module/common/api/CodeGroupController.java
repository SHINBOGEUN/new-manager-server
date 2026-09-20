package net.vivans.dcim.module.common.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.api.dto.CodeGroupRequest;
import net.vivans.dcim.module.common.api.dto.CodeGroupResponse;
import net.vivans.dcim.module.common.application.CodeGroupQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/code-groups")
@Tag(name = "code-groups", description = "코드 그룹 관련 API")
public class CodeGroupController {

    private final CodeGroupQueryService codeGroupQueryService;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "코드 그룹 생성 API")
    @PostMapping
    public ResponseEntity<ApiResponse<CodeGroupResponse>> createCodeGroup(@Valid @RequestBody CodeGroupRequest codeGroupRequest) {
        return ResponseEntity.ok(ApiResponse.ok(codeGroupQueryService.createCodeGroup(codeGroupRequest)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "코드 그룹 수정 API")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CodeGroupResponse>> updateCodeGroup(
            @Parameter(description = "코드 그룹 ID") @PathVariable("id") Integer id,
            @Valid @RequestBody CodeGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(codeGroupQueryService.updateCodeGroup(id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "코드 그룹 삭제 API")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Integer>> deleteCodeGroup(
            @Parameter(description = "코드 그룹 ID") @PathVariable Integer id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(codeGroupQueryService.deleteCodeGroup(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "코드 그룹 전체 목록 조회 API")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CodeGroupResponse>>> getCodeGroups() {
        return ResponseEntity.ok(ApiResponse.ok(codeGroupQueryService.findAll()));
    }
}
