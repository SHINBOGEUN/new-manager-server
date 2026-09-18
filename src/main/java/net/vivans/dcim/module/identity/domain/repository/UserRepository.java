package net.vivans.dcim.module.identity.domain.repository;

import net.vivans.dcim.module.identity.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsername(String username);

    Optional<User> findByRefreshToken(String refreshToken);

    Optional<User> findById(Integer userId);

    List<User> findAllByOrderByUsernameAsc();

    User save(User user);

    void delete(User user);
}
