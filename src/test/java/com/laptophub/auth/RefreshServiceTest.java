package com.laptophub.auth;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.security.AccessToken;
import com.laptophub.security.AccessTokenService;
import com.laptophub.user.User;
import com.laptophub.user.UserRole;
import com.laptophub.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserService userService;

    @Mock
    private AccessTokenService accessTokenService;

    private static final Instant FIXED_NOW = Instant.parse("2026-07-17T12:00:00Z");
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final RefreshTokenProperties refreshTokenProperties = new RefreshTokenProperties(Duration.ofDays(30));

    private RefreshService refreshService;

    @BeforeEach
    void setUp() {
        refreshService = new RefreshService(
                refreshTokenRepository, refreshTokenProperties, userService, accessTokenService, clock);
    }

    private User activeUser(long id) {
        User user = User.create("user@example.com", "hash", "Name", null, UserRole.CUSTOMER);
        user.setId(id);
        return user;
    }

    @Test
    void refresh_validToken_rotatesAndIssuesNewAccessToken() {
        RefreshToken existing = RefreshToken.create(
                7L, "old-hash", "family-1", FIXED_NOW.plus(Duration.ofDays(10)), FIXED_NOW.minus(Duration.ofDays(20)));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(userService.findById(7L)).thenReturn(Optional.of(activeUser(7L)));
        when(accessTokenService.issue(any())).thenReturn(new AccessToken("new-jwt", FIXED_NOW.plus(Duration.ofMinutes(15))));
        RefreshToken savedNewToken = RefreshToken.create(7L, "new-hash", "family-1", FIXED_NOW.plus(Duration.ofDays(30)), FIXED_NOW);
        // RefreshToken.id chỉ được set thật qua IDENTITY generation của DB
        // (không có setter) — test không dùng DB nên set trực tiếp field qua
        // reflection để mô phỏng "đã persist, có id sinh ra".
        ReflectionTestUtils.setField(savedNewToken, "id", 99L);
        when(refreshTokenRepository.save(any())).thenReturn(savedNewToken);

        LoginResult result = refreshService.refresh("raw-old-token");

        assertThat(result.response().accessToken()).isEqualTo("new-jwt");
        assertThat(result.response().expiresIn()).isEqualTo(900L);
        assertThat(result.rawRefreshToken()).isNotBlank();

        // existing bị revoke đúng lý do và trỏ sang token mới.
        assertThat(existing.getRevokedAt()).isEqualTo(FIXED_NOW);
        assertThat(existing.getRevokeReason()).isEqualTo(RevokeReason.ROTATED);
        assertThat(existing.getReplacedByTokenId()).isEqualTo(99L);

        verify(refreshTokenRepository, never()).revokeActiveByFamilyId(any(), any(), any());
    }

    @Test
    void refresh_unknownToken_throwsUnauthenticated() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshService.refresh("unknown-raw-token"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_reusedToken_revokesWholeFamily_andThrowsUnauthenticated() {
        RefreshToken existing = RefreshToken.create(
                7L, "old-hash", "family-2", FIXED_NOW.plus(Duration.ofDays(10)), FIXED_NOW.minus(Duration.ofDays(20)));
        existing.revoke(FIXED_NOW.minus(Duration.ofMinutes(5)), RevokeReason.ROTATED);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> refreshService.refresh("raw-already-used-token"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));

        ArgumentCaptor<RevokeReason> reasonCaptor = ArgumentCaptor.forClass(RevokeReason.class);
        verify(refreshTokenRepository).revokeActiveByFamilyId(eq("family-2"), eq(FIXED_NOW), reasonCaptor.capture());
        assertThat(reasonCaptor.getValue()).isEqualTo(RevokeReason.REUSE_DETECTED);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_expiredToken_throwsUnauthenticated_withoutTouchingFamily() {
        RefreshToken existing = RefreshToken.create(
                7L, "old-hash", "family-3", FIXED_NOW.minus(Duration.ofMinutes(1)), FIXED_NOW.minus(Duration.ofDays(30)));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> refreshService.refresh("raw-expired-token"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));

        verify(refreshTokenRepository, never()).revokeActiveByFamilyId(any(), any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_blockedUser_throwsUnauthenticated_notAccountBlocked() {
        RefreshToken existing = RefreshToken.create(
                7L, "old-hash", "family-4", FIXED_NOW.plus(Duration.ofDays(10)), FIXED_NOW.minus(Duration.ofDays(20)));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        User blockedUser = activeUser(7L);
        blockedUser.block();
        when(userService.findById(7L)).thenReturn(Optional.of(blockedUser));

        assertThatThrownBy(() -> refreshService.refresh("raw-token-of-blocked-user"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));

        verify(refreshTokenRepository, never()).save(any());
    }
}
