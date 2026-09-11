package net.vivans.dcim.module.common.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.api.dto.CommonCodeRequest;
import net.vivans.dcim.module.common.api.dto.CommonCodeResponse;
import net.vivans.dcim.module.common.application.CommonCodeQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/common-codes")
@RequiredArgsConstructor
@Tag(name = "common-codes", description = "공통 코드 관련 API")
public class CommonCodeController {

    private final CommonCodeQueryService commonCodeQueryService;

    @PostMapping
    @Operation(summary = "공통 코드 등록 API")
    public ResponseEntity<ApiResponse<CommonCodeResponse>> createCommonCode(
            @Valid @RequestBody CommonCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(commonCodeQueryService.createCommonCode(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "공통 코드 수정 API")
    public ResponseEntity<ApiResponse<CommonCodeResponse>> updateCommonCode (
            @Parameter(description = "공통 코드 ID") @PathVariable Integer id, @Valid @RequestBody CommonCodeRequest request){
        return ResponseEntity.ok(ApiResponse.ok(commonCodeQueryService.updateCommonCode(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "공통 코드 삭제 API")
    public ResponseEntity<ApiResponse<Integer>> deleteCommonCode(
            @Parameter(description = "공통 코드 ID") @PathVariable Integer id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(commonCodeQueryService.deleteCommonCode(id)));
    }

    @GetMapping
    @Operation(summary = "공통 코드 조회 API")
    public ResponseEntity<ApiResponse<List<CommonCodeResponse>>> getCommonCodeList(
            @Parameter(description = "코드 그룹 ID") @RequestParam(required = false) Integer codeGroupId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(commonCodeQueryService.getCommonCodeList(codeGroupId)));
    }
}
