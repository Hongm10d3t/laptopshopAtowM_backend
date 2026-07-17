package com.laptophub.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// Crypto helper thuần — không phụ thuộc persistence, không log token/hash.
public final class RefreshTokenHasher {

    private static final String ALGORITHM = "SHA-256";

    private RefreshTokenHasher() {
    }

    // Hex thường (lowercase), cố định 64 ký tự — khớp cột token_hash
    // VARCHAR(64) đã định nghĩa ở V2 migration (ASU-19). Không dùng thuật
    // toán chậm có chủ đích như BCrypt cho password: refresh token đã có
    // entropy cao sẵn (sinh ngẫu nhiên 256-bit qua RefreshTokenGenerator,
    // không do người dùng chọn) — chỉ cần hash nhanh, một chiều để tránh lộ
    // token gốc nếu DB bị đọc trộm.
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có sẵn trong JCA provider mặc định của mọi JVM
            // chuẩn — nhánh này về lý thuyết không bao giờ chạy.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
