package net.vivans.dcim.module.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.vivans.dcim.module.identity.domain.model.UserRole;

public record UserManagementCreateRequest(
        @Schema(example = "operator01")
        @NotBlank(message = "username is required")
        @Size(max = 100, message = "username must be at most 100 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 100, message = "password must be 8 to 100 characters")
        String password,

        @NotNull(message = "role is required")
        UserRole role
) {
}
