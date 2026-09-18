package net.vivans.dcim.module.identity.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.identity.api.dto.TokenResponse;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import net.vivans.dcim.shared.security.JwtProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthQueryService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public TokenResponse validate(String accessToken) {
        String username = jwtProvider.extractUsername(accessToken);
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return TokenResponse.of(user, accessToken);
    }
}
