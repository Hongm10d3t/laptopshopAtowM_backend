package com.laptophub.auth;

// Không có refresh token ở đây — refresh token chỉ đi qua Set-Cookie
// (RefreshTokenCookieFactory), không bao giờ vào JSON body. Đúng shape đã
// chốt ở AUTH_SECURITY_USER_CONTRACT.md mục 7.
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
