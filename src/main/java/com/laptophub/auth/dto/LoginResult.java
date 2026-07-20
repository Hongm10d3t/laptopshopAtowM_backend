package com.laptophub.auth.dto;

// Public (không còn "nội bộ package" như trước khi chia lại thư mục theo
// tầng — auth.service và auth (AuthController) giờ là 2 package khác nhau)
// — mang cả LoginResponse (JSON body) lẫn raw refresh token (chỉ controller
// mới được cầm để set cookie). LoginService/RefreshService cố tình không tự
// set cookie: service không phụ thuộc HTTP, đúng nguyên tắc theo suốt từ
// RegisterService.
public record LoginResult(LoginResponse response, String rawRefreshToken) {
}
