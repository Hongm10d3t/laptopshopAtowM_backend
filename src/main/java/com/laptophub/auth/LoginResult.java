package com.laptophub.auth;

// Nội bộ package — mang cả LoginResponse (JSON body) lẫn raw refresh token
// (chỉ controller mới được cầm để set cookie). LoginService cố tình không tự
// set cookie: service không phụ thuộc HTTP, đúng nguyên tắc theo suốt từ
// RegisterService.
record LoginResult(LoginResponse response, String rawRefreshToken) {
}
