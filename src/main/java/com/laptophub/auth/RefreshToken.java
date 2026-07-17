package com.laptophub.auth;

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
// Long thuần thay vì @ManyToOne — gói này chỉ định nghĩa schema/entity
// (không tạo repository/service), chưa có chỗ nào cần navigate quan hệ nên
// tránh phức tạp lazy-loading không cần thiết.
//
// Cố tình KHÔNG thêm method đổi trạng thái (revoke/replace) ở gói này —
// chữ ký chính xác (nhận Instant từ Clock injected hay không, gộp
// revokedAt+replacedByTokenId+revokeReason vào 1 lời gọi hay tách riêng)
// nên thiết kế cùng lúc với service rotation/refresh thật, tránh đoán sai
// rồi phải sửa lại.
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
}
