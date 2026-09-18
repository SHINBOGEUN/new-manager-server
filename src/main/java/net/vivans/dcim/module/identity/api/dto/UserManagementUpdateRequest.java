package net.vivans.dcim.module.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import net.vivans.dcim.module.identity.domain.model.UserRole;

public record UserManagementUpdateRequest(
        @Schema(description = "입력한 경우에만 비밀번호를 변경합니다")
        @Size(min = 8, max = 100, message = "password must be 8 to 100 characters")
        String password,

        UserRole role
) {
}
