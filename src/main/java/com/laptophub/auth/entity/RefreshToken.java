package com.laptophub.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

// Không kế thừa BaseEntity: bảng này không có cột updated_at — vòng đời chỉ
// tạo một lần, các thay đổi sau đó đi qua đúng field có chủ đích
// (revokedAt/replacedByTokenId/revokeReason), không phải audit "sửa lần
// cuối lúc nào" chung chung như User.
//
// userId/replacedByTokenId là FK ở DB (xem V2 migration) nhưng map bằng
// Long thuần thay vì @ManyToOne — chưa có navigate quan hệ nào thật sự cần
// dùng, tránh phức tạp lazy-loading không cần thiết.
//
// revoke()/markReplacedBy() nhận Instant từ caller (không tự gọi
// Instant.now()) — khớp pattern Clock injected đã dùng ở AccessTokenService,
// giúp service rotation/refresh (ASU-23/24) test được deterministic.
@Entity
@Table(name = "refresh_tokens")
@Getter
@ToString(exclude = "tokenHash")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_id")
    private Long replacedByTokenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoke_reason", length = 30)
    private RevokeReason revokeReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private RefreshToken(Long userId, String tokenHash, String familyId, Instant expiresAt, Instant createdAt) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        this.familyId = Objects.requireNonNull(familyId, "familyId must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static RefreshToken create(Long userId, String tokenHash, String familyId, Instant expiresAt,
                                       Instant createdAt) {
        return new RefreshToken(userId, tokenHash, familyId, expiresAt, createdAt);
    }

    // Dùng cho logout/logout-all/reuse detection/rotation. revokedAt truyền
    // vào từ caller (Clock injected) thay vì Instant.now() ở đây.
    public void revoke(Instant revokedAt, RevokeReason reason) {
        this.revokedAt = Objects.requireNonNull(revokedAt, "revokedAt must not be null");
        this.revokeReason = Objects.requireNonNull(reason, "reason must not be null");
    }

    // Chỉ dùng lúc rotation (ASU-24): gọi sau khi đã tạo token mới và biết
    // id của nó, cùng với revoke(now, ROTATED) trên chính token này.
    public void markReplacedBy(Long newTokenId) {
        this.replacedByTokenId = Objects.requireNonNull(newTokenId, "newTokenId must not be null");
    }
}
