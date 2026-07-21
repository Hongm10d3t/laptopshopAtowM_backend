package com.laptophub.security;

import com.laptophub.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

// Component duy nhất trong project được phép ký/parse access JWT. Claim chỉ
// gồm sub (userId), email, role, iss, iat, exp, jti — không có phone, address
// hay password: token có thể bị bên thứ ba đọc được (chỉ ký, không mã hóa),
// nên chỉ đưa dữ liệu tối thiểu cần cho xác thực/phân quyền mỗi request
// (đã chốt ở AUTH_SECURITY_USER_CONTRACT.md, gói ASU-00).
@Service
public class AccessTokenService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey signingKey;

    public AccessTokenService(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        // Keys.hmacShaKeyFor tự ném WeakKeyException nếu secret < 256 bit —
        // đảm bảo "HS256 secret đủ mạnh" ngay lúc khởi tạo bean, không đợi
        // đến lúc ký token đầu tiên mới phát hiện secret yếu.
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public AccessToken issue(UserPrincipal principal) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());

        String token = Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .claim(CLAIM_EMAIL, principal.getUsername())
                .claim(CLAIM_ROLE, principal.getRole().name())
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                // Chỉ định thuật toán tường minh (không chỉ signWith(key)): nếu
                // secret dài hơn 32 byte, JJWT có thể tự chọn HS384/HS512 —
                // ASU-00 đã chốt HS256 nên phải ép rõ ràng.
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        return new AccessToken(token, expiresAt);
    }

    public AccessTokenClaims parse(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(jwtProperties.issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidAccessTokenException("Access token không hợp lệ", e);
        }

        String subject = claims.getSubject();
        String email = claims.get(CLAIM_EMAIL, String.class);
        String role = claims.get(CLAIM_ROLE, String.class);
        String jti = claims.getId();

        if (subject == null || email == null || role == null || jti == null) {
            throw new InvalidAccessTokenException("Access token thiếu claim bắt buộc", null);
        }

        return new AccessTokenClaims(Long.valueOf(subject), email, UserRole.valueOf(role), jti);
    }
}
