-- token_hash lưu SHA-256 hex digest (64 ký tự) của refresh token opaque —
-- không bao giờ lưu token gốc. Khác password_hash của users (BCrypt, cố ý
-- chậm để chống brute-force mật khẩu do người dùng chọn): refresh token đã
-- có entropy cao sẵn (random, không do người dùng đặt) nên chỉ cần hash
-- nhanh để chống lộ khi DB bị đọc trộm, không cần cố ý làm chậm.
--
-- family_id nhóm các token cùng một chuỗi rotation (mỗi lần refresh tạo
-- token mới, replaced_by_token_id trỏ từ token cũ sang token mới). Khi phát
-- hiện reuse (token đã bị rotate nhưng vẫn có người dùng lại), thu hồi cả
-- family bằng một UPDATE theo family_id thay vì phải dò từng bản ghi qua
-- replaced_by_token_id.
--
-- created_at/expires_at/revoked_at dùng DATETIME(6) như bảng users (V1) —
-- tránh giới hạn năm 2038 của TIMESTAMP, nhất quán với serverTimezone=UTC.

CREATE TABLE refresh_tokens (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    user_id               BIGINT       NOT NULL,
    token_hash            VARCHAR(64)  NOT NULL,
    family_id             VARCHAR(36)  NOT NULL,
    expires_at            DATETIME(6)  NOT NULL,
    revoked_at            DATETIME(6)  NULL,
    replaced_by_token_id  BIGINT       NULL,
    revoke_reason         VARCHAR(30)  NULL,
    created_at            DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_refresh_tokens_replaced_by
        FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (id),
    CONSTRAINT ck_refresh_tokens_revoke_reason
        CHECK (revoke_reason IN ('LOGOUT', 'LOGOUT_ALL', 'ROTATED', 'REUSE_DETECTED')),

    INDEX idx_refresh_tokens_user_id (user_id),
    INDEX idx_refresh_tokens_family_id (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
