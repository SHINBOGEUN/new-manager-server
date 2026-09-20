package net.vivans.dcim.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.bootstrap.ManagerServerApplication;
import net.vivans.dcim.module.identity.domain.model.UserRole;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static net.vivans.dcim.support.AuthTestSupport.bearerToken;
import static net.vivans.dcim.support.AuthTestSupport.loginAndGetAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 역할별 접근 권한(RBAC) 작업 검증.
 * <p>
 * - ADMIN: 모든 관리 API에 접근 가능
 * - USER: 조회 + 수집 작업 시작/중지는 가능하지만 등록·수정·삭제, 사용자관리, 공통코드,
 *         MQTT 모니터는 불가
 * - GUEST: 조회만 가능. 수집 작업 시작/중지를 포함한 어떤 변경도 불가하고,
 *          사용자관리·공통코드·MQTT 모니터는 조회조차 불가
 * - 미인증 요청은 401, 권한 부족은 403
 * <p>
 * 실제 등록/수정 로직 자체(성공 케이스)는 각 컨트롤러의 기존 통합 테스트가 이미 검증하므로,
 * 이 테스트는 권한 경계(403/401 여부)만 확인한다. 존재하지 않는 ID를 사용하는 케이스는
 * Spring Security의 {@code @PreAuthorize}가 컨트롤러 메서드 진입 전에 평가되므로, 대상 리소스가
 * 없어도 권한 응답(403)은 그대로 검증할 수 있다.
 */
@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class RolePermissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String token(String username, UserRole role) throws Exception {
        return loginAndGetAccessToken(mockMvc, objectMapper, userRepository, username, "password123", role);
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/manager/devices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanAccessUserManagementButUserAndGuestCannot() throws Exception {
        String admin = token("rbac-admin-users", UserRole.ADMIN);
        String user = token("rbac-user-users", UserRole.USER);
        String guest = token("rbac-guest-users", UserRole.GUEST);

        mockMvc.perform(get("/api/manager/users").header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/manager/users").header("Authorization", bearerToken(user)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/users").header("Authorization", bearerToken(guest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void everyRoleCanViewDeviceList() throws Exception {
        String admin = token("rbac-admin-devices", UserRole.ADMIN);
        String user = token("rbac-user-devices", UserRole.USER);
        String guest = token("rbac-guest-devices", UserRole.GUEST);

        for (String accessToken : new String[] {admin, user, guest}) {
            mockMvc.perform(get("/api/manager/devices").header("Authorization", bearerToken(accessToken)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void onlyAdminCanRegisterDeviceModel() throws Exception {
        String user = token("rbac-user-model-create", UserRole.USER);
        String guest = token("rbac-guest-model-create", UserRole.GUEST);
        String body = """
                {"name":"RBAC-TEST-MODEL","manufacturer":"ACME","deviceTypeId":1,"protocols":[{"protocolTypeId":1}]}
                """;

        mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(guest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminCanDeleteDevice() throws Exception {
        String user = token("rbac-user-device-delete", UserRole.USER);
        String guest = token("rbac-guest-device-delete", UserRole.GUEST);

        mockMvc.perform(delete("/api/manager/devices/{id}", 999_999)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/manager/devices/{id}", 999_999)
                        .header("Authorization", bearerToken(guest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanToggleCollectionTaskButGuestCannot() throws Exception {
        String user = token("rbac-user-task-toggle", UserRole.USER);
        String guest = token("rbac-guest-task-toggle", UserRole.GUEST);

        // 존재하지 않는 taskId라도 @PreAuthorize는 컨트롤러 진입 전에 평가되므로,
        // USER는 403이 아닌 다른 응답(대상 없음 등)을 받아야 "권한은 있다"가 증명된다.
        int userStatus = mockMvc.perform(patch("/api/manager/collector/tasks/{taskId}/toggle", 999_999)
                        .header("Authorization", bearerToken(user)))
                .andReturn().getResponse().getStatus();
        assertThat(userStatus).isNotEqualTo(403);

        mockMvc.perform(patch("/api/manager/collector/tasks/{taskId}/toggle", 999_999)
                        .header("Authorization", bearerToken(guest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminAndUserCanAccessCommonCodesButGuestCannot() throws Exception {
        String user = token("rbac-user-common-codes", UserRole.USER);
        String guest = token("rbac-guest-common-codes", UserRole.GUEST);

        mockMvc.perform(get("/api/manager/common-codes").header("Authorization", bearerToken(user)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/manager/common-codes").header("Authorization", bearerToken(guest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userAndGuestCannotAccessMqttMonitor() throws Exception {
        String user = token("rbac-user-mqtt", UserRole.USER);
        String guest = token("rbac-guest-mqtt", UserRole.GUEST);
        String body = """
                {"host":"127.0.0.1","port":1883,"topic":"dcim/test"}
                """;

        mockMvc.perform(post("/api/manager/mqtt-monitor/sessions/{sessionId}", "rbac-session")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/manager/mqtt-monitor/sessions/{sessionId}", "rbac-session")
                        .header("Authorization", bearerToken(guest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
