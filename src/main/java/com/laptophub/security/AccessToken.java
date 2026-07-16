package com.laptophub.security;

import java.time.Instant;

public record AccessToken(String token, Instant expiresAt) {
}
