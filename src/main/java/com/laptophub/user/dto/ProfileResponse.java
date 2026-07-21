package com.laptophub.user.dto;

import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.entity.UserStatus;

import java.time.Instant;

public record ProfileResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        UserRole role,
        UserStatus status,
        Instant createdAt) {

    public static ProfileResponse from(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
