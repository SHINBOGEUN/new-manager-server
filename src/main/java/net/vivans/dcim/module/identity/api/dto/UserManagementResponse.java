package net.vivans.dcim.module.identity.api.dto;

import net.vivans.dcim.module.identity.domain.model.User;
import net.vivans.dcim.module.identity.domain.model.UserRole;

import java.time.Instant;

public record UserManagementResponse(
        Integer id,
        String username,
        UserRole role,
        Instant createdDt,
        Instant updatedDt
) {
    public static UserManagementResponse from(User user) {
        return new UserManagementResponse(
                user.getId(),
                user.getUsername(),
                UserRole.from(user.getRole()),
                user.getCreatedDt(),
                user.getUpdatedDt()
        );
    }
}
