package net.vivans.dcim.module.identity.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.identity.api.dto.TokenResponse;
import net.vivans.dcim.module.identity.api.dto.UserResponse;
import net.vivans.dcim.module.identity.domain.model.User;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import net.vivans.dcim.module.identity.infrastructure.security.CustomUserDetails;
import net.vivans.dcim.shared.exception.InvalidTokenException;
import net.vivans.dcim.shared.security.JwtProvider;
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
        String encodedPassword = passwordEncoder.encode(password);
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> User.createNew(username, encodedPassword));
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
