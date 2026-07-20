package com.laptophub.auth.entity;

public enum RevokeReason {
    LOGOUT,
    LOGOUT_ALL,
    ROTATED,
    REUSE_DETECTED,
    // Đăng nhập mới thu hồi phiên cũ của cùng user (chỉ 1 phiên/user —
    // AUTH_SECURITY_USER_CONTRACT.md mục 4). Khác LOGOUT/LOGOUT_ALL (hành
    // động chủ động của người dùng) — đây là hệ quả tự động của login mới.
    NEW_LOGIN
}
