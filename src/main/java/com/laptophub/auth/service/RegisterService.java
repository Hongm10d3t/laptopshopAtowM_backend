package com.laptophub.auth.service;

import com.laptophub.auth.dto.RegisterRequest;
import com.laptophub.auth.dto.RegisterResponse;
import com.laptophub.user.EmailNormalizer;
import com.laptophub.user.entity.User;
import com.laptophub.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Business use case đăng ký Customer — chưa expose HTTP (không import gì từ
// jakarta.servlet/Spring MVC, không phụ thuộc request/response). Controller
// gói sau sẽ gọi thẳng method register(). Không tự query UserRepository —
// đi qua UserService đúng ranh giới ASU-00.
@Service
public class RegisterService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public RegisterService(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        // Hash NGAY tại đây, trước khi chạm vào bất kỳ thành phần nào của
        // user module — passwordHash là thứ duy nhất đi xa hơn điểm này,
        // raw password dừng lại ở method này.
        String passwordHash = passwordEncoder.encode(request.password());

        User user = userService.createCustomer(normalizedEmail, passwordHash, request.fullName(), request.phone());

        return new RegisterResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone());
    }
}
