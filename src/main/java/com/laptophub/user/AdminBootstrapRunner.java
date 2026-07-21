package com.laptophub.user;

import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.repository.UserRepository;
import com.laptophub.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Seed đúng 1 tài khoản ADMIN đầu tiên qua biến môi trường ADMIN_EMAIL/
// ADMIN_PASSWORD (quyết định đã chốt cùng người dùng, không có API tạo ADMIN
// công khai). Idempotent: chỉ tạo nếu DB CHƯA có ADMIN nào — an toàn khi app
// restart nhiều lần, không tạo trùng dù chạy lại bao nhiêu lần.
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 72;

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrapRunner(UserRepository userRepository, UserService userService,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${app.admin.email}") String adminEmail,
                                 @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }

        String normalizedEmail = EmailNormalizer.normalize(adminEmail);
        validatePassword(adminPassword);

        String passwordHash = passwordEncoder.encode(adminPassword);
        User admin = userService.createAdmin(normalizedEmail, passwordHash, "Administrator");

        log.info("Đã tạo tài khoản ADMIN đầu tiên: id={}, email={}", admin.getId(), admin.getEmail());
    }

    // Fail-fast lúc khởi động thay vì âm thầm tạo ADMIN với mật khẩu quá ngắn
    // (cùng policy min/max với RegisterRequest, ASU-16) — ADMIN_PASSWORD đến
    // từ biến môi trường nên không đi qua Bean Validation của DTO.
    private void validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD phải từ " + PASSWORD_MIN_LENGTH + " đến " + PASSWORD_MAX_LENGTH + " ký tự");
        }
    }
}
