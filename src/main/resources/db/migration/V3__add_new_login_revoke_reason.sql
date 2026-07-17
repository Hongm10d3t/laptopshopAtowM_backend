-- Thêm 'NEW_LOGIN' vào danh sách revoke_reason hợp lệ: đăng nhập mới thu hồi
-- phiên cũ của cùng user (chỉ 1 phiên/user), khác với LOGOUT/LOGOUT_ALL
-- (hành động chủ động của người dùng) hay ROTATED/REUSE_DETECTED.

ALTER TABLE refresh_tokens DROP CHECK ck_refresh_tokens_revoke_reason;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT ck_refresh_tokens_revoke_reason
        CHECK (revoke_reason IN ('LOGOUT', 'LOGOUT_ALL', 'ROTATED', 'REUSE_DETECTED', 'NEW_LOGIN'));
