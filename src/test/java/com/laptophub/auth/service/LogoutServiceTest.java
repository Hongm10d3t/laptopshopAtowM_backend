package com.laptophub.auth.service;

import com.laptophub.auth.entity.RevokeReason;
import com.laptophub.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private static final Instant FIXED_NOW = Instant.parse("2026-07-17T13:00:00Z");
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Test
    void logout_revokesActiveTokensForUser_withGivenReason() {
        LogoutService logoutService = new LogoutService(refreshTokenRepository, clock);

        logoutService.logout(7L, RevokeReason.LOGOUT);

        verify(refreshTokenRepository).revokeActiveByUserId(7L, FIXED_NOW, RevokeReason.LOGOUT);
    }

    @Test
    void logoutAll_usesLogoutAllReason() {
        LogoutService logoutService = new LogoutService(refreshTokenRepository, clock);

        logoutService.logout(7L, RevokeReason.LOGOUT_ALL);

        verify(refreshTokenRepository).revokeActiveByUserId(7L, FIXED_NOW, RevokeReason.LOGOUT_ALL);
    }
}
