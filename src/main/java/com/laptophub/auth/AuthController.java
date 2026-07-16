package com.laptophub.auth;

import com.laptophub.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controller mỏng: chỉ nhận request, validate, gọi service — business logic
// nằm hết ở RegisterService (đúng PROJECT_RULES.md mục 3). Không khai
// "/api/v1" vì context-path đã cắt sẵn (AUTH_SECURITY_USER_CONTRACT.md mục 8).
// "/auth/**" đã permitAll trong SecurityConfig từ ASU-09 — endpoint anonymous
// mà không cần cấu hình gì thêm ở đây.
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterService registerService;

    public AuthController(RegisterService registerService) {
        this.registerService = registerService;
    }

    // Chỉ tạo account, KHÔNG phát access/refresh token — đúng contract đã
    // chốt (login là hành động riêng, register không tự động đăng nhập).
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
