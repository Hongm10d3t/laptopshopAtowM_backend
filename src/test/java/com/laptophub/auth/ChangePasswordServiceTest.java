package com.laptophub.auth;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.security.SecurityConfig;
import com.laptophub.user.User;
import com.laptophub.user.UserRole;
import com.laptophub.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    // PasswordEncoder thật (không mock) — cần hành vi matches/encode thật,
    // giống RegisterServiceTest/RefreshServiceTest.
    private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

    private static final Instant FIXED_NOW = Instant.parse("2026-07-17T13:00:00Z");
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private ChangePasswordService changePasswordService;

    @BeforeEach
    void setUp() {
        changePasswordService =
                new ChangePasswordService(userService, passwordEncoder, refreshTokenRepository, clock);
    }

    private User userWithPassword(String rawPassword) {
        User user = User.create("user@example.com", passwordEncoder.encode(rawPassword), "Name", null,
                UserRole.CUSTOMER);
        user.setId(7L);
        return user;
    }

    @Test
    void changePassword_correctCurrentPassword_updatesHash_andRevokesRefreshTokens() {
        User user = userWithPassword("OldSecret1!");
        when(userService.findById(7L)).thenReturn(Optional.of(user));
        String oldHash = user.getPasswordHash();

        changePasswordService.changePassword(7L, new ChangePasswordRequest("OldSecret1!", "NewSecret2!"));

        assertThat(user.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(passwordEncoder.matches("NewSecret2!", user.getPasswordHash())).isTrue();
        verify(refreshTokenRepository).revokeActiveByUserId(7L, FIXED_NOW, RevokeReason.LOGOUT_ALL);
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsInvalidCredentials_andDoesNotChangeOrRevoke() {
        User user = userWithPassword("OldSecret1!");
        when(userService.findById(7L)).thenReturn(Optional.of(user));
        String oldHash = user.getPasswordHash();

        assertThatThrownBy(() ->
                changePasswordService.changePassword(7L, new ChangePasswordRequest("WrongPassword!", "NewSecret2!")))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        assertThat(user.getPasswordHash()).isEqualTo(oldHash);
        verify(refreshTokenRepository, never()).revokeActiveByUserId(any(), any(), any());
    }

    @Test
    void changePassword_unknownUserId_throwsUnauthenticated() {
        when(userService.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                changePasswordService.changePassword(99L, new ChangePasswordRequest("OldSecret1!", "NewSecret2!")))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHENTICATED));

        verify(refreshTokenRepository, never()).revokeActiveByUserId(any(), any(), any());
    }
}
