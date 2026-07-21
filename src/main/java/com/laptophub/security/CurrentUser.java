package com.laptophub.security;

import com.laptophub.user.entity.UserRole;

public record CurrentUser(Long userId, String email, UserRole role) {
}
