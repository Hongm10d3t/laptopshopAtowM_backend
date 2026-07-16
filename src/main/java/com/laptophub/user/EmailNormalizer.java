package com.laptophub.user;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    // null/blank bị coi là lỗi lập trình (caller phải validate trước, vd @NotBlank),
    // không trả về rỗng để tránh normalize âm thầm ra giá trị vô nghĩa.
    public static String normalize(String email) {
        if (email == null) {
            throw new IllegalArgumentException("email must not be null");
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
