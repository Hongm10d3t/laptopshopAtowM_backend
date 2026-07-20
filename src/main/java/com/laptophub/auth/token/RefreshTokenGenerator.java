package com.laptophub.auth.token;

import java.security.SecureRandom;
import java.util.Base64;

// Crypto helper thuần — không phụ thuộc persistence (không import gì từ
// RefreshToken/RefreshTokenRepository), không log token sinh ra ở bất kỳ
// đâu trong class này.
public final class RefreshTokenGenerator {

    // 256 bit = 32 byte, đúng tối thiểu yêu cầu.
    private static final int TOKEN_BYTE_LENGTH = 32;

    // SecureRandom (không phải java.util.Random — không đủ ngẫu nhiên cho
    // mục đích bảo mật, trạng thái nội bộ có thể bị đoán/tái tạo). Instance
    // dùng chung: SecureRandom vốn thread-safe, tái sử dụng còn giúp entropy
    // pool được trộn liên tục qua nhiều lần gọi thay vì mỗi lần tạo mới.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RefreshTokenGenerator() {
    }

    // URL-safe Base64, không padding (chốt ở gói này): token có thể nằm
    // trong cookie hoặc URL mà không cần percent-encode ký tự '=', '+', '/'.
    public static String generate() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
