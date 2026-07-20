package com.laptophub.auth.service;

import com.laptophub.auth.entity.RevokeReason;
import com.laptophub.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

// Business use case logout/logout-all — không phụ thuộc HTTP (không đọc/xóa
// cookie, đó là việc của AuthController). Dùng lại bulk revoke có sẵn từ
// ASU-20 — không cần biết token cụ thể nào, chỉ cần userId từ access token
// đã xác thực (CurrentUserProvider ở tầng controller), nên vẫn đúng dù cookie
// refresh token bị thiếu/sai/đã hết hạn.
@Service
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public LogoutService(RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Transactional
    public void logout(Long userId, RevokeReason reason) {
        refreshTokenRepository.revokeActiveByUserId(userId, clock.instant(), reason);
    }
}
