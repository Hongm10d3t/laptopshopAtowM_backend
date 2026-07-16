package com.laptophub.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cố tình KHÔNG có role/status/userId — role luôn là CUSTOMER và status luôn
// ACTIVE khi tự đăng ký, service tự gán cứng, không đọc từ client (đúng
// API_CONVENTION.md mục 4: "Không tin ... role hoặc trạng thái do frontend
// gửi lên"). phone không bắt buộc, khớp cột nullable của bảng users.
public record RegisterRequest(

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        // min=8 theo khuyến nghị NIST/OWASP hiện nay (ưu tiên độ dài hơn ép
        // buộc hoa/thường/ký tự đặc biệt); max=72 vì BCrypt (đã chọn ở
        // SecurityConfig) chỉ dùng 72 byte đầu, phần dư bị lặng lẽ bỏ qua —
        // chặn ở validation để không tạo ảo giác mật khẩu dài hơn có tác dụng.
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
        String fullName,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String phone) {
}
