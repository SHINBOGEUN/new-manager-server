package net.vivans.dcim.module.identity.infrastructure.persistence;

import net.vivans.dcim.module.identity.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDataRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    Optional<User> findByRefreshToken(String refreshToken);

    List<User> findAllByOrderByUsernameAsc();
}
