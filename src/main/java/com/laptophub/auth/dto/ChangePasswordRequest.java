package com.laptophub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// currentPassword cố tình KHÔNG có @Size — đây là mật khẩu CŨ, có thể đã được
// tạo trước khi policy hiện tại có hiệu lực, không được ép policy mới lên nó
// (giống lý do LoginRequest không @Size password).
public record ChangePasswordRequest(

        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        String currentPassword,

        // Cùng policy min/max với RegisterRequest.password (ASU-16).
        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu mới phải từ 8 đến 72 ký tự")
        String newPassword) {
}
