package net.vivans.dcim.module.identity.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import net.vivans.dcim.bootstrap.ManagerServerApplication;
import net.vivans.dcim.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static net.vivans.dcim.support.AuthTestSupport.loginAndGetRefreshCookie;
import static net.vivans.dcim.support.AuthTestSupport.loginAndGetResponse;
import static net.vivans.dcim.support.AuthTestSupport.registerUser;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AuthControllerIntegrationTest {

    /** AuthController의 REFRESH_TOKEN_COOKIE와 동일한 이름 (private 상수라 여기선 리터럴로 검증). */
    private static final String REFRESH_TOKEN_COOKIE = "manager_refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_returnsCreatedUser() throws Exception {
        mockMvc.perform(post("/api/manager/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"register-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.username").value("register-user"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void login_returnsAccessTokenAndSetsHttpOnlyRefreshCookie() throws Exception {
        registerUser(mockMvc, "login-user", "password123");

        mockMvc.perform(post("/api/manager/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"login-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("login-user"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                // Refresh Token은 응답 본문이 아니라 HttpOnly 쿠키로만 내려간다.
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(REFRESH_TOKEN_COOKIE + "=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    void login_returnsUnauthorizedForWrongPassword() throws Exception {
        registerUser(mockMvc, "wrong-pass-user", "password123");

        mockMvc.perform(post("/api/manager/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"wrong-pass-user","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void validate_returnsTokenInfo() throws Exception {
        JsonNode loginResponse = loginAndGetResponse(mockMvc, objectMapper, "validate-user", "password123");
        String accessToken = loginResponse.get("accessToken").asText();

        mockMvc.perform(get("/api/manager/auth/validate")
                        .param("accessToken", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("validate-user"))
                .andExpect(jsonPath("$.data.accessToken").value(accessToken));
    }

    @Test
    void refresh_returnsNewAccessTokenAndRotatesRefreshCookie() throws Exception {
        Cookie loginCookie = loginAndGetRefreshCookie(mockMvc, "refresh-user", "password123");

        mockMvc.perform(post("/api/manager/auth/refresh")
                        .cookie(loginCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("refresh-user"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(REFRESH_TOKEN_COOKIE + "=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    void logout_clearsRefreshTokenCookie() throws Exception {
        Cookie loginCookie = loginAndGetRefreshCookie(mockMvc, "logout-user", "password123");

        mockMvc.perform(post("/api/manager/auth/logout")
                        .cookie(loginCookie))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(REFRESH_TOKEN_COOKIE + "=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }
}
