package net.vivans.dcim.module.identity.application;

import net.vivans.dcim.module.identity.api.dto.TokenResponse;
import net.vivans.dcim.module.identity.domain.model.User;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import net.vivans.dcim.shared.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthQueryService authQueryService;

    @Test
    void validate_returnsTokenResponseForValidAccessToken() {
        User user = User.createNew("testuser", "encoded-password");
        user.updateRefreshToken("stored-refresh-token");

        given(jwtProvider.extractUsername("access-token")).willReturn("testuser");
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));

        TokenResponse response = authQueryService.validate("access-token");

        // TokenResponse에는 refreshToken 필드가 없다 (Refresh Token은 HttpOnly 쿠키로만 내려간다).
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void validate_throwsWhenUserNotFound() {
        given(jwtProvider.extractUsername("access-token")).willReturn("unknown");
        given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authQueryService.validate("access-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

}
