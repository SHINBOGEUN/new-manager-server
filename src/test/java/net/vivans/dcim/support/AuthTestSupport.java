package net.vivans.dcim.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import net.vivans.dcim.module.identity.domain.model.User;
import net.vivans.dcim.module.identity.domain.model.UserRole;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class AuthTestSupport {

    /** AuthController의 REFRESH_TOKEN_COOKIE와 동일한 이름. Refresh Token은 이제 이 쿠키로만 내려간다. */
    private static final String REFRESH_TOKEN_COOKIE = "manager_refresh_token";

    private AuthTestSupport() {
    }

    /** 로그인 후, 응답 JSON이 아니라 Set-Cookie로 내려온 HttpOnly Refresh Token 쿠키를 그대로 돌려준다. */
    public static Cookie loginAndGetRefreshCookie(MockMvc mockMvc, String username, String password) throws Exception {
        registerUser(mockMvc, username, password);

        MvcResult result = mockMvc.perform(post("/api/manager/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(REFRESH_TOKEN_COOKIE);
        if (cookie == null) {
            throw new IllegalStateException("login response did not set the refresh token cookie");
        }
        return cookie;
    }

    public static void registerUser(MockMvc mockMvc, String username, String password) throws Exception {
        mockMvc.perform(post("/api/manager/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password)));
    }

    public static JsonNode loginAndGetResponse(MockMvc mockMvc, ObjectMapper objectMapper, String username, String password) throws Exception {
        registerUser(mockMvc, username, password);

        MvcResult result = mockMvc.perform(post("/api/manager/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    public static String loginAndGetAccessToken(MockMvc mockMvc, ObjectMapper objectMapper, String username, String password) throws Exception {
        return loginAndGetResponse(mockMvc, objectMapper, username, password).get("accessToken").asText();
    }

    public static String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    /**
     * RBAC(역할별 접근 권한) 적용 이후, device/model/task 등 관리 API는 ADMIN 권한이 있어야
     * 호출할 수 있다. 기존 통합 테스트 대부분은 이 헬퍼로 만든 사용자로 데이터를 준비하므로,
     * 별도 역할 지정 없이 호출하면 항상 ADMIN으로 등록·로그인한다.
     */
    public static String loginAndGetAccessToken(
            MockMvc mockMvc, ObjectMapper objectMapper, UserRepository userRepository,
            String username, String password
    ) throws Exception {
        return loginAndGetAccessToken(mockMvc, objectMapper, userRepository, username, password, UserRole.ADMIN);
    }

    /** ADMIN이 아닌 다른 역할(USER, GUEST)로 로그인 토큰이 필요한 권한 테스트 전용 오버로드. */
    public static String loginAndGetAccessToken(
            MockMvc mockMvc, ObjectMapper objectMapper, UserRepository userRepository,
            String username, String password, UserRole role
    ) throws Exception {
        registerUser(mockMvc, username, password);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("test user not found after register: " + username));
        user.changeRole(role);
        userRepository.save(user);
        return loginAndGetAccessToken(mockMvc, objectMapper, username, password);
    }
}
