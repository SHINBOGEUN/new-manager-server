package net.vivans.dcim.module.identity.api.dto;

import net.vivans.dcim.module.identity.domain.model.User;

public record TokenResponse(
        String username,
        String role,
        String accessToken
) {

    public static TokenResponse of(User user, String accessToken) {
        return new TokenResponse(user.getUsername(), user.getRole(), accessToken);
    }
}
