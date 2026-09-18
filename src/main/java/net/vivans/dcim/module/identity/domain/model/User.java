package net.vivans.dcim.module.identity.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;
    private String password;
    private String role;
    private String refreshToken;

    private User(String username, String password, String role, String refreshToken) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.refreshToken = refreshToken;
    }

    public static User createNew(String username, String encodedPassword) {
        return createNew(username, encodedPassword, UserRole.USER);
    }

    public static User createNew(String username, String encodedPassword, UserRole role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        return new User(username.trim(), encodedPassword, role.name(), null);
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void changeRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        this.role = role.name();
    }

    public void changePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        this.password = encodedPassword;
    }
}
