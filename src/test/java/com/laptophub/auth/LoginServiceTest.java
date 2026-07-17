package com.laptophub.auth;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.security.AccessToken;
import com.laptophub.security.AccessTokenService;
import com.laptophub.security.UserPrincipal;
import com.laptophub.user.User;
import com.laptophub.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AccessTokenService accessTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private static final Instant FIXED_NOW = Instant.parse("2026-07-17T10:00:00Z");
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final RefreshTokenProperties refreshTokenProperties = new RefreshTokenProperties(Duration.ofDays(30));

    // Không dùng field initializer: với MockitoExtension, field initializer
    // chạy TRƯỚC khi @Mock được inject, nên loginService sẽ giữ tham chiếu
    // null. Phải khởi tạo trong @BeforeEach, chạy SAU khi mock đã sẵn sàng.
    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(
                authenticationManager, accessTokenService, refreshTokenRepository, refreshTokenProperties, clock);
    }

    private UserPrincipal principal(long id) {
        User user = User.create("user@example.com", "hash", "Name", null, UserRole.CUSTOMER);
        user.setId(id);
        return UserPrincipal.from(user);
    }

    @Test
    void login_success_returnsAccessToken_revokesOldSession_andPersistsNewRefreshToken() {
        UserPrincipal principal = principal(7L);
        UsernamePasswordAuthenticationToken authenticated =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authenticated);
        when(accessTokenService.issue(principal))
                .thenReturn(new AccessToken("jwt-value", FIXED_NOW.plus(Duration.ofMinutes(15))));

        LoginResult result = loginService.login(new LoginRequest("user@example.com", "correct-password"));

        assertThat(result.response().accessToken()).isEqualTo("jwt-value");
        assertThat(result.response().tokenType()).isEqualTo("Bearer");
        assertThat(result.response().expiresIn()).isEqualTo(900L);
        assertThat(result.rawRefreshToken()).isNotBlank();

        verify(refreshTokenRepository).revokeActiveByUserId(7L, FIXED_NOW, RevokeReason.NEW_LOGIN);

        ArgumentCaptor<RefreshToken> savedCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(savedCaptor.capture());
        RefreshToken saved = savedCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getTokenHash()).isEqualTo(RefreshTokenHasher.hash(result.rawRefreshToken()));
        assertThat(saved.getExpiresAt()).isEqualTo(FIXED_NOW.plus(Duration.ofDays(30)));
        assertThat(saved.getCreatedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials_andNeverTouchesRefreshTokens() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> loginService.login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(refreshTokenRepository, never()).revokeActiveByUserId(any(), any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_blockedAccount_throwsAccountBlocked() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("blocked"));

        assertThatThrownBy(() -> loginService.login(new LoginRequest("user@example.com", "correct-password")))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_BLOCKED));

        verify(refreshTokenRepository, never()).save(any());
    }
}
