package net.vivans.dcim.module.identity.domain.model;

import java.util.Locale;

public enum UserRole {
    ADMIN,
    USER,
    GUEST;

    public static UserRole from(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        try {
            return UserRole.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported user role: " + value);
        }
    }
}
