package com.laptophub.auth.token;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Nơi duy nhất build Set-Cookie cho refresh token — mọi thuộc tính cookie
// (HttpOnly/Secure/SameSite/Path) chốt ở đây, tránh mỗi endpoint tự set một
// kiểu khác nhau. Path=/api/v1/auth khớp URL bên ngoài thật (context-path
// /api/v1 + "/auth") — đây là path trình duyệt nhìn thấy, không phải path
// nội bộ Spring MVC sau khi đã cắt context-path.
//
// Secure=true cố định, không theo profile: trình duyệt hiện đại coi
// localhost là secure context nên cookie Secure vẫn hoạt động khi test qua
// browser tại http://localhost; Postman/curl không thực thi cờ Secure nên
// cũng không bị chặn khi test thủ công.
@Component
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final RefreshTokenProperties properties;

    public RefreshTokenCookieFactory(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie build(String rawToken) {
        return baseCookie(rawToken)
                .maxAge(properties.ttl())
                .build();
    }

    // Max-Age=0 để trình duyệt xóa cookie ngay — dùng lúc logout/logout-all.
    public ResponseCookie clear() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(COOKIE_PATH);
    }
}
