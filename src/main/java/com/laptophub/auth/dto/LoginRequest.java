package com.laptophub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Không dùng @Size cho password như RegisterRequest: login chỉ cần biết có
// khớp mật khẩu đã lưu hay không (qua AuthenticationManager), không phải
// chỗ để ép lại password policy hiện tại — mật khẩu cũ hợp lệ tại thời điểm
// tạo vẫn phải đăng nhập được dù policy có đổi sau này.
public record LoginRequest(

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password) {
}
