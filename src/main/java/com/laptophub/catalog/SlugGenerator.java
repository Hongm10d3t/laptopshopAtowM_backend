package com.laptophub.catalog;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

// Dùng chung cho Category/Brand/Product: sinh slug từ tên, luôn chạy ở tầng
// service dù Admin có tự nhập slug hay không — đảm bảo slug hợp lệ bất kể
// input. 'đ'/'Đ' không bị NFD tách thành ký tự gốc + dấu (khác các nguyên âm
// có dấu khác) nên phải thay thủ công trước khi bỏ dấu.
public final class SlugGenerator {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");

    private SlugGenerator() {
    }

    public static String generate(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("input must not be null or blank");
        }

        String withoutD = input.replace('đ', 'd').replace('Đ', 'D');
        String normalized = Normalizer.normalize(withoutD, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        String lower = withoutDiacritics.toLowerCase(Locale.ROOT);
        String hyphenated = NON_ALPHANUMERIC.matcher(lower).replaceAll("-");
        return EDGE_HYPHENS.matcher(hyphenated).replaceAll("");
    }
}
