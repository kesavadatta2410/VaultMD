package com.vaultmd.backend.dto;

import com.vaultmd.backend.model.Role;

public class AuthResponse {

    private final String token;
    private final Long userId;
    private final String fullName;
    private final Role role;

    public AuthResponse(String token, Long userId, String fullName, Role role) {
        this.token = token;
        this.userId = userId;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }
}
