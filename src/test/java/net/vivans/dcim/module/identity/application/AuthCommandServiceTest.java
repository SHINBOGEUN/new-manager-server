package net.vivans.dcim.module.identity.application;

import net.vivans.dcim.module.identity.api.dto.UserResponse;
import net.vivans.dcim.module.identity.domain.model.User;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import net.vivans.dcim.module.identity.infrastructure.security.CustomUserDetails;
import net.vivans.dcim.shared.exception.InvalidTokenException;
import net.vivans.dcim.shared.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AuthCommandService authCommandService;

    @Test
    void register_whenPublicRegistrationDisabled_firstAccountBecomesAdmin() {
        given(jdbcTemplate.queryForList("SELECT id FROM initial_admin_lock WHERE id = 1 FOR UPDATE", Integer.class)).willReturn(List.of(1));
        given(userRepository.findAllByOrderByUsernameAsc()).willReturn(List.of());
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");
        given(userRepository.findByUsername("admin")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authCommandService.register("admin", "password123");

        assertThat(response.role()).isEqualTo("ADMIN");
        InOrder order = inOrder(jdbcTemplate, userRepository);
        order.verify(jdbcTemplate).queryForList("SELECT id FROM initial_admin_lock WHERE id = 1 FOR UPDATE", Integer.class);
        order.verify(userRepository).findAllByOrderByUsernameAsc();
        assertThat(authCommandService.registrationPolicy().initialAdmin()).isTrue();
    }

    @Test
    void register_whenPublicRegistrationDisabled_rejectsAfterFirstAccount() {
        given(jdbcTemplate.queryForList("SELECT id FROM initial_admin_lock WHERE id = 1 FOR UPDATE", Integer.class)).willReturn(List.of(1));
        given(userRepository.findAllByOrderByUsernameAsc()).willReturn(List.of(User.createNew("admin", "encoded")));

        assertThatThrownBy(() -> authCommandService.register("intruder", "password123"))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(authCommandService.registrationPolicy().open()).isFalse();
    }

    @Test
    void register_whenBootstrapLockMissing_failsClosed() {
        given(jdbcTemplate.queryForList("SELECT id FROM initial_admin_lock WHERE id = 1 FOR UPDATE", Integer.class)).willReturn(List.of());
        assertThatThrownBy(() -> authCommandService.register("admin", "password123"))
                .isInstanceOf(IllegalStateException.class);
        org.mockito.Mockito.verifyNoInteractions(userRepository);
    }

    @Test
    void register_createsNewUser() {
        ReflectionTestUtils.setField(authCommandService, "publicRegistrationEnabled", true);
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");
        given(userRepository.findByUsername("newuser")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authCommandService.register("newuser", "password123");

        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.role()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_returnsExistingUserWithoutUpdatingPassword() {
        ReflectionTestUtils.setField(authCommandService, "publicRegistrationEnabled", true);
        User existing = User.createNew("existing", "encoded-password");
        given(passwordEncoder.encode("any-password")).willReturn("new-encoded-password");
        given(userRepository.findByUsername("existing")).willReturn(Optional.of(existing));
        given(userRepository.save(existing)).willReturn(existing);

        UserResponse response = authCommandService.register("existing", "any-password");

        assertThat(response.username()).isEqualTo("existing");
        assertThat(existing.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void login_issuesTokensAndSavesRefreshToken() {
        User user = User.createNew("testuser", "encoded-password");
        CustomUserDetails userDetails = new CustomUserDetails(user);
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtProvider.generateToken(userDetails)).willReturn("access-token");
        given(jwtProvider.generateRefreshToken(userDetails)).willReturn("refresh-token");
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(userRepository.save(user)).willReturn(user);

        AuthTokenIssue issue = authCommandService.login("testuser", "password123");

        assertThat(issue.tokenResponse().username()).isEqualTo("testuser");
        assertThat(issue.tokenResponse().accessToken()).isEqualTo("access-token");
        assertThat(issue.refreshToken()).isEqualTo("refresh-token");
        assertThat(user.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void refresh_issuesNewTokensWhenRefreshTokenIsValid() {
        User user = User.createNew("testuser", "encoded-password");
        user.updateRefreshToken("old-refresh-token");

        given(userRepository.findByRefreshToken("old-refresh-token")).willReturn(Optional.of(user));
        given(jwtProvider.validateToken(eq("old-refresh-token"), any(CustomUserDetails.class))).willReturn(true);
        given(jwtProvider.generateToken(any(CustomUserDetails.class))).willReturn("new-access-token");
        given(jwtProvider.generateRefreshToken(any(CustomUserDetails.class))).willReturn("new-refresh-token");
        given(userRepository.save(user)).willReturn(user);

        AuthTokenIssue issue = authCommandService.refresh("old-refresh-token");

        assertThat(issue.tokenResponse().accessToken()).isEqualTo("new-access-token");
        assertThat(issue.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(user.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void refresh_throwsWhenRefreshTokenNotFound() {
        given(userRepository.findByRefreshToken("unknown-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authCommandService.refresh("unknown-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void refresh_throwsWhenJwtValidationFails() {
        User user = User.createNew("testuser", "encoded-password");
        user.updateRefreshToken("invalid-refresh-token");
        CustomUserDetails userDetails = new CustomUserDetails(user);

        given(userRepository.findByRefreshToken("invalid-refresh-token")).willReturn(Optional.of(user));
        given(jwtProvider.validateToken(eq("invalid-refresh-token"), any(CustomUserDetails.class))).willReturn(false);

        assertThatThrownBy(() -> authCommandService.refresh("invalid-refresh-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid refresh token");
    }

}
