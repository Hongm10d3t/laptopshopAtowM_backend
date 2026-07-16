package com.laptophub.user;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Cổng đọc tối thiểu cho module khác (vd security) — không được tự query
// UserRepository trực tiếp, phải đi qua đây (ranh giới đã chốt ở
// AUTH_SECURITY_USER_CONTRACT.md, gói ASU-00).
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Caller (vd CustomUserDetailsService) phải tự normalize qua EmailNormalizer
    // trước khi gọi — method này không normalize giúp.
    public Optional<User> findByNormalizedEmail(String normalizedEmail) {
        return userRepository.findByEmail(normalizedEmail);
    }

    // Dùng bởi JwtAuthenticationFilter: token chỉ mang userId (claim sub),
    // nên mỗi request phải tải lại user mới nhất theo ID để bắt kịp status
    // BLOCKED phát sinh sau khi token đã phát hành.
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Luôn hardcode role CUSTOMER — không nhận role từ caller nên không thể
    // tạo ADMIN qua đường này (đăng ký công khai). Caller (RegisterService)
    // phải tự normalize email và hash password trước khi gọi; method này
    // không làm giúp, chỉ lo phần persistence.
    //
    // Check trùng email 2 lớp:
    // 1) existsByEmail trước — chặn sớm, tránh round-trip insert thất bại ở
    //    trường hợp thường gặp.
    // 2) bắt DataIntegrityViolationException khi saveAndFlush — cho trường
    //    hợp race: 2 request cùng qua được bước (1) trước khi request nào
    //    insert trước (UNIQUE constraint uk_users_email chặn ở DB).
    // Cả 2 lớp đều map về cùng EMAIL_ALREADY_EXISTS (409), không lọt thành
    // lỗi 500 ở tầng trên. Dùng saveAndFlush (không phải save) để ép SQL
    // INSERT chạy ngay trong try-catch này — nếu chỉ save(), lỗi ràng buộc
    // có thể trôi tới lúc transaction commit, ngoài phạm vi catch.
    @Transactional
    public User createCustomer(String normalizedEmail, String passwordHash, String fullName, String phone) {
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.create(normalizedEmail, passwordHash, fullName, phone, UserRole.CUSTOMER);

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }
}
