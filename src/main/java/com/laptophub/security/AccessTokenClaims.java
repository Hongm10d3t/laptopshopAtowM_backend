package com.laptophub.security;

import com.laptophub.user.UserRole;

public record AccessTokenClaims(Long userId, String email, UserRole role, String jti) {
}
