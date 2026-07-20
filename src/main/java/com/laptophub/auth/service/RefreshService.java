package com.laptophub.auth.service;

import com.laptophub.auth.dto.LoginResponse;
import com.laptophub.auth.dto.LoginResult;
import com.laptophub.auth.entity.RefreshToken;
import com.laptophub.auth.entity.RevokeReason;
import com.laptophub.auth.repository.RefreshTokenRepository;
import com.laptophub.auth.token.RefreshTokenGenerator;
import com.laptophub.auth.token.RefreshTokenHasher;
import com.laptophub.auth.token.RefreshTokenProperties;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.security.AccessToken;
import com.laptophub.security.AccessTokenService;
import com.laptophub.security.UserPrincipal;
import com.laptophub.user.User;
import com.laptophub.user.UserService;
import com.laptophub.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

// Business use case refresh token — không phụ thuộc HTTP (không đọc cookie,
// đó là việc của AuthController). Mọi lý do từ chối đều ném cùng
// AppException(UNAUTHENTICATED) — không tiết lộ ra client token thiếu, hết
// hạn hay bị dùng lại (AUTH_SECURITY_USER_CONTRACT.md mục 5).
@Service
public class RefreshService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties refreshTokenProperties;
    private final UserService userService;
    private final AccessTokenService accessTokenService;
    private final Clock clock;

    public RefreshService(RefreshTokenRepository refreshTokenRepository,
                           RefreshTokenProperties refreshTokenProperties,
                           UserService userService,
                           AccessTokenService accessTokenService,
                           Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenProperties = refreshTokenProperties;
        this.userService = userService;
        this.accessTokenService = accessTokenService;
        this.clock = clock;
    }

    // noRollbackFor=AppException: mặc định Spring rollback TOÀN BỘ transaction
    // khi có RuntimeException thoát ra, kể cả các câu lệnh đã chạy trước đó
    // trong cùng transaction — nếu không khai rõ, nhánh reuse detection bên
    // dưới sẽ bị "vô hiệu hóa" ngược: revokeActiveByFamilyId() chạy xong,
    // nhưng vì ngay sau đó ném AppException nên toàn bộ UPDATE đó bị rollback,
    // family không thực sự bị thu hồi trong DB dù request trả 401 (đã tự
    // phát hiện bug này qua verify app thật, không phải suy đoán). Các nhánh
    // throw khác trong method này không có mutation nào trước đó nên không bị
    // ảnh hưởng bởi việc tắt rollback.
    @Transactional(noRollbackFor = AppException.class)
    public LoginResult refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        String tokenHash = RefreshTokenHasher.hash(rawRefreshToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (existing.getRevokedAt() != null) {
            // Token đã bị dùng/rotate/revoke trước đó mà vẫn được gửi lên
            // lần nữa -> dấu hiệu lộ token. Thu hồi cả family, không chỉ
            // đúng token này.
            refreshTokenRepository.revokeActiveByFamilyId(existing.getFamilyId(), now, RevokeReason.REUSE_DETECTED);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (existing.getExpiresAt().isBefore(now)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Tải lại user mới nhất — user bị BLOCKED sau khi refresh token phát
        // hành vẫn phải bị từ chối, giống nguyên tắc JwtAuthenticationFilter
        // áp dụng cho access token (ASU-12).
        User user = userService.findById(existing.getUserId())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        String newRawToken = RefreshTokenGenerator.generate();
        String newTokenHash = RefreshTokenHasher.hash(newRawToken);
        Instant newExpiresAt = now.plus(refreshTokenProperties.ttl());
        RefreshToken newToken = refreshTokenRepository.save(
                RefreshToken.create(existing.getUserId(), newTokenHash, existing.getFamilyId(), newExpiresAt, now));

        // existing vẫn là entity managed trong transaction hiện tại (load qua
        // findByTokenHash) — chỉ cần mutate, Hibernate tự UPDATE lúc flush,
        // không cần gọi save() lại.
        existing.revoke(now, RevokeReason.ROTATED);
        existing.markReplacedBy(newToken.getId());

        AccessToken accessToken = accessTokenService.issue(UserPrincipal.from(user));
        long expiresInSeconds = Duration.between(now, accessToken.expiresAt()).toSeconds();

        LoginResponse response = new LoginResponse(accessToken.token(), "Bearer", expiresInSeconds);
        return new LoginResult(response, newRawToken);
    }
}
