package net.vivans.dcim.module.identity.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.identity.api.dto.UserManagementCreateRequest;
import net.vivans.dcim.module.identity.api.dto.UserManagementResponse;
import net.vivans.dcim.module.identity.api.dto.UserManagementUpdateRequest;
import net.vivans.dcim.module.identity.application.UserManagementService;
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
@RequestMapping("/api/manager/users")
@Tag(name = "user-management", description = "운영 사용자 계정 관리 API")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @Operation(summary = "사용자 목록 조회")
    public ResponseEntity<ApiResponse<List<UserManagementResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.getUsers()));
    }

    @PostMapping
    @Operation(summary = "운영 사용자 생성")
    public ResponseEntity<ApiResponse<UserManagementResponse>> createUser(
            @Valid @RequestBody UserManagementCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.createUser(request)));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "사용자 역할 또는 비밀번호 변경")
    public ResponseEntity<ApiResponse<UserManagementResponse>> updateUser(
            @PathVariable Integer userId,
            @Valid @RequestBody UserManagementUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.updateUser(userId, request)));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 삭제")
    public ResponseEntity<ApiResponse<Integer>> deleteUser(@PathVariable Integer userId) {
        userManagementService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(userId));
    }
}
