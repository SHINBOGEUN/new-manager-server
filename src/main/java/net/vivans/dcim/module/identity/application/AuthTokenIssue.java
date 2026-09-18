package net.vivans.dcim.module.identity.application;

import net.vivans.dcim.module.identity.api.dto.TokenResponse;

/**
 * Access Token 응답과 HttpOnly 쿠키에만 실을 Refresh Token을 분리한다.
 */
public record AuthTokenIssue(
        TokenResponse tokenResponse,
        String refreshToken
) {
}
