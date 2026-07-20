package com.laptophub.auth.token;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieFactoryTest {

    private final RefreshTokenCookieFactory factory =
            new RefreshTokenCookieFactory(new RefreshTokenProperties(Duration.ofDays(30)));

    @Test
    void build_setsExpectedAttributes() {
        ResponseCookie cookie = factory.build("raw-refresh-token-value");

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEqualTo("raw-refresh-token-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void clear_hasZeroMaxAgeAndEmptyValue() {
        ResponseCookie cookie = factory.clear();

        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookie.getValue()).isEmpty();
        // Vẫn giữ đúng name/path/attribute để trình duyệt nhận diện đúng
        // cookie cần xóa (khớp cookie đã set lúc login).
        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
    }
}
