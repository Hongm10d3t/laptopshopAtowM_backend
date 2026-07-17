package com.laptophub.auth;

import com.laptophub.common.ApiResponse;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controller mỏng: chỉ nhận request, validate, gọi service, set cookie —
// business logic nằm hết ở RegisterService/LoginService (đúng PROJECT_RULES.md
// mục 3). Không khai "/api/v1" vì context-path đã cắt sẵn
// (AUTH_SECURITY_USER_CONTRACT.md mục 8). Route rule (permitAll cho
// register/login/refresh, authenticated cho phần còn lại) khai ở SecurityConfig,
// không lặp lại ở đây.
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterService registerService;
    private final LoginService loginService;
    private final RefreshService refreshService;
    private final LogoutService logoutService;
    private final CurrentUserProvider currentUserProvider;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    public AuthController(RegisterService registerService, LoginService loginService,
                           RefreshService refreshService, LogoutService logoutService,
                           CurrentUserProvider currentUserProvider,
                           RefreshTokenCookieFactory refreshTokenCookieFactory) {
        this.registerService = registerService;
        this.loginService = loginService;
        this.refreshService = refreshService;
        this.logoutService = logoutService;
        this.currentUserProvider = currentUserProvider;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    // Chỉ tạo account, KHÔNG phát access/refresh token — đúng contract đã
    // chốt (login là hành động riêng, register không tự động đăng nhập).
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                              HttpServletResponse httpResponse) {
        LoginResult result = loginService.login(request);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE,
                refreshTokenCookieFactory.build(result.rawRefreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(result.response()));
    }

    // Thiếu cookie -> từ chối ngay ở tầng controller, cùng loại lỗi
    // (UNAUTHENTICATED) mà RefreshService dùng cho mọi lý do từ chối khác —
    // không có cách nào phân biệt "thiếu cookie" với "token hết hạn/bị dùng
    // lại" từ phía client.
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String rawRefreshToken,
            HttpServletResponse httpResponse) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        LoginResult result = refreshService.refresh(rawRefreshToken);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE,
                refreshTokenCookieFactory.build(result.rawRefreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(result.response()));
    }

    // Lấy userId qua CurrentUserProvider (đã xác thực bằng access token, do
    // SecurityConfig yêu cầu authenticated cho path này) — không đọc/tin
    // cookie refresh token để xác định "ai" đang logout, nên vẫn đúng dù
    // cookie thiếu/sai/hết hạn. Idempotent: revokeActiveByUserId trả 0 nếu
    // không còn phiên nào active, không phải lỗi.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        logoutService.logout(currentUserProvider.getCurrentUser().userId(), RevokeReason.LOGOUT);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString());
        return ResponseEntity.noContent().build();
    }

    // Với single-session (ASU-00), logout-all hiện tương đương logout — vẫn
    // tách endpoint và RevokeReason riêng để không phải đổi contract khi sau
    // này mở multi-device.
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(HttpServletResponse httpResponse) {
        logoutService.logout(currentUserProvider.getCurrentUser().userId(), RevokeReason.LOGOUT_ALL);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString());
        return ResponseEntity.noContent().build();
    }
}
