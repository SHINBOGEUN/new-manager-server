package net.vivans.dcim.module.identity.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.identity.api.dto.AuthRequest;
import net.vivans.dcim.module.identity.api.dto.TokenResponse;
import net.vivans.dcim.module.identity.api.dto.UserResponse;
import net.vivans.dcim.module.identity.application.AuthTokenIssue;
import net.vivans.dcim.module.identity.application.AuthCommandService;
import net.vivans.dcim.module.identity.application.AuthQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import net.vivans.dcim.shared.security.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/auth")
@Tag(name = "auth", description = "인증 관련 API")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "manager_refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/manager/auth";

    private final AuthCommandService authCommandService;
    private final AuthQueryService authQueryService;
    private final JwtProperties jwtProperties;

    @Value("${security.refresh-cookie.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${security.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @Operation(summary = "회원 아이디와 비밀번호를 입력받아 토큰을 발급하는 API")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody AuthRequest request) {
        AuthTokenIssue issued = authCommandService.login(request.username(), request.password());
        return withRefreshCookie(issued);
    }

    @Operation(summary = "회원 아이디와 비밀번호를 입력받아 신규 회원을 추가하는 API")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody AuthRequest request) {
        UserResponse user = authCommandService.register(request.username(), request.password());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @Operation(summary = "HttpOnly Refresh Token 쿠키로 새 Access Token을 발급하는 API")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken
    ) {
        AuthTokenIssue issued = authCommandService.refresh(refreshToken);
        return withRefreshCookie(issued);
    }

    @Operation(summary = "현재 브라우저의 Refresh Token을 폐기하고 로그아웃하는 API")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken
    ) {
        authCommandService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .body(ApiResponse.ok(null));
    }

    @Operation(summary = "엑세스 토큰 검증을 위한 API")
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<TokenResponse>> validate(
            @Parameter(required = true) @RequestParam String accessToken
    ) {
        TokenResponse token = authQueryService.validate(accessToken);
        return ResponseEntity.ok(ApiResponse.ok(token));
    }

    private ResponseEntity<ApiResponse<TokenResponse>> withRefreshCookie(AuthTokenIssue issued) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(issued.refreshToken()).toString())
                .body(ApiResponse.ok(issued.tokenResponse()));
    }

    private ResponseCookie refreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofSeconds(jwtProperties.getRefreshTokenExpiration()))
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
