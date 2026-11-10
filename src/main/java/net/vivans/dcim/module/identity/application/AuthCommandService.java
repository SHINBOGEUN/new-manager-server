package net.vivans.dcim.module.identity.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.identity.api.dto.TokenResponse;
import net.vivans.dcim.module.identity.api.dto.UserResponse;
import net.vivans.dcim.module.identity.domain.model.User;
import net.vivans.dcim.module.identity.domain.model.UserRole;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import net.vivans.dcim.module.identity.infrastructure.security.CustomUserDetails;
import net.vivans.dcim.shared.exception.InvalidTokenException;
import net.vivans.dcim.shared.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthCommandService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 공개 회원가입 허용 여부(PUBLIC_REGISTRATION_ENABLED, 기본 false).
     * 꺼져 있으면 사용자가 한 명도 없을 때의 첫 가입만 허용하고, 그 계정은 ADMIN이 된다(빈 현장 초기 관리자 생성).
     * 이후 계정은 관리자가 사용자 관리(POST /api/manager/users)에서 만든다.
     */
    @Value("${security.public-registration-enabled:false}")
    private boolean publicRegistrationEnabled;

    /** 로그인 화면이 회원가입 버튼을 보여줄지 판단할 때 쓴다. */
    @Transactional(readOnly = true)
    public RegistrationPolicy registrationPolicy() {
        if (publicRegistrationEnabled) return new RegistrationPolicy(true, false);
        boolean firstUser = userRepository.findAllByOrderByUsernameAsc().isEmpty();
        return new RegistrationPolicy(firstUser, firstUser);
    }

    public record RegistrationPolicy(boolean open, boolean initialAdmin) {}

    @Transactional
    public AuthTokenIssue login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtProvider.generateToken(userDetails);
        String refreshToken = jwtProvider.generateRefreshToken(userDetails);

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
        user.updateRefreshToken(refreshToken);
        userRepository.save(user);

        return new AuthTokenIssue(TokenResponse.of(user, accessToken), refreshToken);
    }

    @Transactional
    public UserResponse register(String username, String password) {
        UserRole role = UserRole.USER;
        if (!publicRegistrationEnabled) {
            // The fixed row serializes bootstrap across instances until this transaction commits.
            var lockRows = jdbcTemplate.queryForList("SELECT id FROM initial_admin_lock WHERE id = 1 FOR UPDATE", Integer.class);
            if (lockRows.size() != 1) throw new IllegalStateException("Initial administrator lock row is missing");
            if (!userRepository.findAllByOrderByUsernameAsc().isEmpty()) {
                throw new AccessDeniedException("회원가입이 꺼져 있습니다. 관리자에게 계정 생성을 요청하세요.");
            }
            role = UserRole.ADMIN;
        }
        String encodedPassword = passwordEncoder.encode(password);
        UserRole newUserRole = role;
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> User.createNew(username, encodedPassword, newUserRole));
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional
    public AuthTokenIssue refresh(String refreshToken) {
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        CustomUserDetails userDetails = new CustomUserDetails(user);

        if (!jwtProvider.validateToken(refreshToken, userDetails)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String newAccessToken = jwtProvider.generateToken(userDetails);
        String newRefreshToken = jwtProvider.generateRefreshToken(userDetails);
        user.updateRefreshToken(newRefreshToken);
        userRepository.save(user);

        return new AuthTokenIssue(TokenResponse.of(user, newAccessToken), newRefreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        userRepository.findByRefreshToken(refreshToken)
                .ifPresent(user -> {
                    user.updateRefreshToken(null);
                    userRepository.save(user);
                });
    }
}
