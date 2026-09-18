package net.vivans.dcim.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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
}
