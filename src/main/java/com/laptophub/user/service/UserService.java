package com.laptophub.user.service;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.entity.UserStatus;
import com.laptophub.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
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

    // Chỉ dùng bởi AdminBootstrapRunner lúc khởi động app — không expose qua
    // API công khai nào. Caller đã tự kiểm tra "chưa có ADMIN nào" trước khi
    // gọi (existsByRole), nên không lặp lại check trùng email/race ở đây như
    // createCustomer.
    @Transactional
    public User createAdmin(String normalizedEmail, String passwordHash, String fullName) {
        User user = User.create(normalizedEmail, passwordHash, fullName, null, UserRole.ADMIN);
        return userRepository.saveAndFlush(user);
    }

    // Customer tự sửa hồ sơ — userId đến từ access token đã xác thực (tầng
    // controller), user luôn tồn tại thật nên orElseThrow chỉ là an toàn
    // phòng vệ.
    @Transactional
    public User updateProfile(Long userId, String fullName, String phone) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        user.updateProfile(fullName, phone);
        return user;
    }

    // Cho Admin liệt kê/tìm kiếm tài khoản — role/status/keyword đều optional.
    public Page<User> search(UserRole role, UserStatus status, String keyword, Pageable pageable) {
        return userRepository.search(role, status, keyword, pageable);
    }

    // actingAdminId khác targetUserId: chặn admin tự khóa chính mình — dự án
    // hiện chỉ seed đúng 1 admin qua biến môi trường, tự khóa sẽ không còn ai
    // mở lại được tài khoản đó qua API.
    @Transactional
    public User blockUser(Long actingAdminId, Long targetUserId) {
        if (Objects.equals(actingAdminId, targetUserId)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể tự khóa tài khoản của chính mình");
        }
        User user = userRepository.findById(targetUserId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        user.block();
        return user;
    }

    @Transactional
    public User activateUser(Long targetUserId) {
        User user = userRepository.findById(targetUserId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        user.activate();
        return user;
    }
}
