package net.vivans.dcim.module.identity.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.identity.api.dto.UserManagementCreateRequest;
import net.vivans.dcim.module.identity.api.dto.UserManagementResponse;
import net.vivans.dcim.module.identity.api.dto.UserManagementUpdateRequest;
import net.vivans.dcim.module.identity.domain.model.User;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserManagementResponse> getUsers() {
        return userRepository.findAllByOrderByUsernameAsc().stream()
                .map(UserManagementResponse::from)
                .toList();
    }

    @Transactional
    public UserManagementResponse createUser(UserManagementCreateRequest request) {
        String username = request.username().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username already exists: " + username);
        }
        User user = User.createNew(username, passwordEncoder.encode(request.password()), request.role());
        return UserManagementResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserManagementResponse updateUser(Integer userId, UserManagementUpdateRequest request) {
        if (request.role() == null && (request.password() == null || request.password().isBlank())) {
            throw new IllegalArgumentException("role or password is required");
        }
        User user = getUser(userId);
        boolean credentialsChanged = false;
        if (request.role() != null) {
            user.changeRole(request.role());
            credentialsChanged = true;
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.changePassword(passwordEncoder.encode(request.password()));
            credentialsChanged = true;
        }
        if (credentialsChanged) {
            // 역할 또는 비밀번호 변경 후에는 기존 로그인 갱신을 허용하지 않는다.
            user.updateRefreshToken(null);
        }
        return UserManagementResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Integer userId) {
        userRepository.delete(getUser(userId));
    }

    private User getUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
    }
}
