package com.laptophub.security;

import com.laptophub.user.User;
import com.laptophub.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Toàn bộ test dùng Clock.fixed(...) — không phụ thuộc System.currentTimeMillis(),
// nên deterministic và không cần Thread.sleep để mô phỏng hết hạn token.
class AccessTokenServiceTest {

    private static final String STRONG_SECRET = "test-jwt-secret-key-at-least-32-bytes-long!";
    private static final String ISSUER = "laptophub-backend-test";
    private static final Instant FIXED_NOW = Instant.parse("2026-07-16T10:00:00Z");

    private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final JwtProperties jwtProperties = new JwtProperties(STRONG_SECRET, ISSUER, Duration.ofMinutes(15));
    private final AccessTokenService accessTokenService = new AccessTokenService(jwtProperties, fixedClock);

    private UserPrincipal principal(UserRole role) {
        User user = User.create("user@example.com", "hash", "Test User", null, role);
        user.setId(42L);
        return UserPrincipal.from(user);
    }

    @Test
    void issueThenParse_roundTripsExpectedClaims() {
        AccessToken issued = accessTokenService.issue(principal(UserRole.ADMIN));

        AccessTokenClaims claims = accessTokenService.parse(issued.token());

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.role()).isEqualTo(UserRole.ADMIN);
        assertThat(claims.jti()).isNotBlank();
        assertThat(issued.expiresAt()).isEqualTo(FIXED_NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void tokenContainsOnlyExpectedClaims_noPhoneAddressOrPassword() {
        AccessToken issued = accessTokenService.issue(principal(UserRole.CUSTOMER));
        SecretKey key = Keys.hmacShaKeyFor(STRONG_SECRET.getBytes(StandardCharsets.UTF_8));

        // Phải gắn .clock(fixedClock) như AccessTokenService làm nội bộ —
        // nếu không, parser dùng giờ hệ thống thật để check exp, và test sẽ
        // ngẫu nhiên fail một khi đồng hồ thật trôi qua FIXED_NOW + 15 phút
        // (đúng thứ ASU-11 yêu cầu tránh: "test không phụ thuộc thời gian hệ
        // thống").
        Claims rawClaims = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(fixedClock.instant()))
                .build()
                .parseSignedClaims(issued.token())
                .getPayload();

        assertThat(rawClaims.keySet()).isEqualTo(Set.of("sub", "email", "role", "iss", "iat", "exp", "jti"));
        assertThat(rawClaims).doesNotContainKeys("phone", "address", "password", "passwordHash");
    }

    @Test
    void expiredToken_isRejected() {
        AccessToken issued = accessTokenService.issue(principal(UserRole.CUSTOMER));

        Clock afterExpiry = Clock.fixed(FIXED_NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC);
        AccessTokenService laterService = new AccessTokenService(jwtProperties, afterExpiry);

        assertThatThrownBy(() -> laterService.parse(issued.token()))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void wrongIssuer_isRejected() {
        SecretKey key = Keys.hmacShaKeyFor(STRONG_SECRET.getBytes(StandardCharsets.UTF_8));
        String tokenWithWrongIssuer = Jwts.builder()
                .subject("42")
                .claim("email", "user@example.com")
                .claim("role", "CUSTOMER")
                .issuer("someone-else")
                .issuedAt(Date.from(FIXED_NOW))
                .expiration(Date.from(FIXED_NOW.plus(Duration.ofMinutes(15))))
                .id("fake-jti")
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> accessTokenService.parse(tokenWithWrongIssuer))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void badSignature_isRejected() {
        SecretKey differentKey = Keys.hmacShaKeyFor("a-completely-different-secret-key-32-bytes+".getBytes(StandardCharsets.UTF_8));
        String tokenSignedWithDifferentKey = Jwts.builder()
                .subject("42")
                .claim("email", "user@example.com")
                .claim("role", "CUSTOMER")
                .issuer(ISSUER)
                .issuedAt(Date.from(FIXED_NOW))
                .expiration(Date.from(FIXED_NOW.plus(Duration.ofMinutes(15))))
                .id("fake-jti")
                .signWith(differentKey, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> accessTokenService.parse(tokenSignedWithDifferentKey))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void weakSecret_rejectedAtConstruction() {
        JwtProperties weakProperties = new JwtProperties("too-short", ISSUER, Duration.ofMinutes(15));

        assertThatThrownBy(() -> new AccessTokenService(weakProperties, fixedClock))
                .isInstanceOf(WeakKeyException.class);
    }
}
