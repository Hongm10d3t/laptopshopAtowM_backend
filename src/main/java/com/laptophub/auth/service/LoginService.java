package com.laptophub.auth.service;

import com.laptophub.auth.dto.LoginRequest;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

// Business use case đăng nhập — không phụ thuộc HTTP (không set cookie, đó
// là việc của AuthController). Dùng lại AuthenticationManager/AccessTokenService
// đã có sẵn từ ASU-10/11, không tự viết logic so mật khẩu.
@Service
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties refreshTokenProperties;
    private final Clock clock;

    public LoginService(AuthenticationManager authenticationManager,
            AccessTokenService accessTokenService,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenProperties refreshTokenProperties,
            Clock clock) {
        this.authenticationManager = authenticationManager;
        this.accessTokenService = accessTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenProperties = refreshTokenProperties;
        this.clock = clock;
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        UserPrincipal principal = authenticate(request);
        Instant now = clock.instant();

        AccessToken accessToken = accessTokenService.issue(principal);
        long expiresInSeconds = Duration.between(now, accessToken.expiresAt()).toSeconds();

        // Chỉ 1 phiên/user — revoke phiên cũ TRONG CÙNG transaction trước
        // khi tạo phiên mới (AUTH_SECURITY_USER_CONTRACT.md mục 4).
        refreshTokenRepository.revokeActiveByUserId(principal.getId(), now, RevokeReason.NEW_LOGIN);

        String rawRefreshToken = RefreshTokenGenerator.generate();
        String tokenHash = RefreshTokenHasher.hash(rawRefreshToken);
        String familyId = UUID.randomUUID().toString();
        Instant refreshExpiresAt = now.plus(refreshTokenProperties.ttl());
        refreshTokenRepository.save(
                RefreshToken.create(principal.getId(), tokenHash, familyId, refreshExpiresAt, now));

        LoginResponse response = new LoginResponse(accessToken.token(), "Bearer", expiresInSeconds);
        return new LoginResult(response, rawRefreshToken);
    }

    // BadCredentialsException phủ cả "email không tồn tại"
    // (DaoAuthenticationProvider
    // với hideUserNotFoundExceptions=true tự quy về đây, xem ASU-10) lẫn "sai
    // mật khẩu" — cùng map về INVALID_CREDENTIALS, không phân biệt ra client.
    // DisabledException (user BLOCKED) tách riêng vì đây là tín hiệu khác,
    // không phải lộ email tồn tại hay không.
    private UserPrincipal authenticate(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            return (UserPrincipal) authentication.getPrincipal();
        } catch (DisabledException e) {
            throw new AppException(ErrorCode.ACCOUNT_BLOCKED);
        } catch (BadCredentialsException e) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
    }
}
