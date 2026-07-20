package com.laptophub.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private RefreshToken newToken() {
        Instant now = Instant.now();
        return RefreshToken.create(1L, "hash-1", "family-1", now.plus(Duration.ofDays(30)), now);
    }

    @Test
    void revoke_setsRevokedAtAndReason() {
        RefreshToken token = newToken();
        Instant revokedAt = Instant.now();

        token.revoke(revokedAt, RevokeReason.LOGOUT);

        assertThat(token.getRevokedAt()).isEqualTo(revokedAt);
        assertThat(token.getRevokeReason()).isEqualTo(RevokeReason.LOGOUT);
    }

    @Test
    void markReplacedBy_setsReplacedByTokenId() {
        RefreshToken token = newToken();

        token.markReplacedBy(42L);

        assertThat(token.getReplacedByTokenId()).isEqualTo(42L);
    }

    @Test
    void rotation_revokeAndMarkReplacedBy_togetherReflectRotatedState() {
        RefreshToken oldToken = newToken();
        Instant now = Instant.now();

        oldToken.revoke(now, RevokeReason.ROTATED);
        oldToken.markReplacedBy(99L);

        assertThat(oldToken.getRevokedAt()).isEqualTo(now);
        assertThat(oldToken.getRevokeReason()).isEqualTo(RevokeReason.ROTATED);
        assertThat(oldToken.getReplacedByTokenId()).isEqualTo(99L);
    }
}
