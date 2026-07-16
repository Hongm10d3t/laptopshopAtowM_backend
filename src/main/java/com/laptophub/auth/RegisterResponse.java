package com.laptophub.auth;

// Không có password/passwordHash/role/status — chỉ xác nhận lại những gì
// không nhạy cảm đã được lưu. DTO thuần, không phải entity (không @Entity,
// không kế thừa BaseEntity).
public record RegisterResponse(Long id, String email, String fullName, String phone) {
}
