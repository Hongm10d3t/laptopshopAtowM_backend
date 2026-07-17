package com.laptophub.auth;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenGeneratorTest {

    @Test
    void generate_returnsNonBlankToken() {
        assertThat(RefreshTokenGenerator.generate()).isNotBlank();
    }

    @Test
    void generate_producesDifferentTokenEachCall() {
        String first = RefreshTokenGenerator.generate();
        String second = RefreshTokenGenerator.generate();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void generate_manyCalls_areAllUnique() {
        Set<String> tokens = new HashSet<>();
        IntStream.range(0, 1000).forEach(i -> tokens.add(RefreshTokenGenerator.generate()));

        assertThat(tokens).hasSize(1000);
    }

    @Test
    void generate_decodesToAtLeast256Bits() {
        String token = RefreshTokenGenerator.generate();

        byte[] decoded = Base64.getUrlDecoder().decode(token);

        assertThat(decoded.length * 8).isGreaterThanOrEqualTo(256);
    }

    @Test
    void generate_isUrlSafeWithoutPadding() {
        String token = RefreshTokenGenerator.generate();

        // Base64 chuẩn (không URL-safe) có thể chứa '+' và '/'; padding
        // dùng '='. Cả 3 ký tự này không được xuất hiện.
        assertThat(token).doesNotContain("+", "/", "=");
        assertThat(token).matches("^[A-Za-z0-9_-]+$");
    }
}
