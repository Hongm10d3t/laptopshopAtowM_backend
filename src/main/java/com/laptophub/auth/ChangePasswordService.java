package com.laptophub.auth;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.user.User;
import com.laptophub.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

// Business use case đổi mật khẩu — không phụ thuộc HTTP (không đọc
// SecurityContext/CurrentUserProvider, đó là việc của AuthController, giống
// pattern LogoutService: nhận userId đã xác thực từ tầng gọi).
@Service
public class ChangePasswordService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public ChangePasswordService(UserService userService, PasswordEncoder passwordEncoder,
                                  RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    // userId đến từ access token đã xác thực (CurrentUserProvider ở tầng
    // controller) nên user luôn tồn tại thật — orElseThrow chỉ là an toàn
    // phòng vệ, không phải luồng nghiệp vụ thường gặp.
    //
    // Revoke refresh token hiện tại giống logout (AUTH_SECURITY_USER_CONTRACT.md
    // mục 6) — access token cũ vẫn dùng được tới khi hết hạn tự nhiên (TTL 15
    // phút), đây là rủi ro đã được chấp nhận ở mức MVP, không cần blacklist.
    //
    // THỨ TỰ QUAN TRỌNG: revokeActiveByUserId() dùng
    // @Modifying(clearAutomatically = true) (RefreshTokenRepository) — gọi
    // xong sẽ entityManager.clear() TOÀN BỘ persistence context. Nếu đổi mật
    // khẩu (mutate `user`) TRƯỚC rồi mới revoke, thay đổi passwordHash sẽ bị
    // clear() loại bỏ lặng lẽ (chưa kịp flush vì Hibernate không thấy bảng
    // `users` liên quan tới câu UPDATE trên bảng `refresh_tokens`) — mật khẩu
    // trông như đã đổi thành công (200 OK) nhưng DB không hề đổi. Tự phát
    // hiện qua verify app thật, không phải suy đoán — cùng lớp bug với
    // RefreshService (transaction rollback), lần này là persistence-context
    // clear. Vì vậy: revoke TRƯỚC, tải lại user (managed mới) rồi mới mutate,
    // để thay đổi được flush đúng lúc transaction commit.
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userService.findById(userId).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        refreshTokenRepository.revokeActiveByUserId(userId, clock.instant(), RevokeReason.LOGOUT_ALL);

        User managedUser = userService.findById(userId).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        managedUser.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }
}
