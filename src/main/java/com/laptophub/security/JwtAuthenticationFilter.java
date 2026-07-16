package com.laptophub.security;

import com.laptophub.user.User;
import com.laptophub.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

// Chỉ xử lý request có "Authorization: Bearer <token>". Không có header hoặc
// sai định dạng -> bỏ qua, đi tiếp chain như request ẩn danh (không phá
// public endpoint). Token có nhưng không hợp lệ theo bất kỳ lý do nào (hết
// hạn, sai chữ ký, issuer sai, thiếu claim, user không tồn tại hoặc BLOCKED)
// -> cũng chỉ đi tiếp như ẩn danh, KHÔNG tự viết response ở đây. Việc trả
// 401 dạng JSON cho request cần xác thực mà chưa xác thực được giao thống
// nhất cho JsonAuthenticationEntryPoint (tránh 2 nơi tự viết response khác
// hình dạng nhau). Không log token ở bất kỳ nhánh nào trong file này.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenService accessTokenService;
    private final UserService userService;

    public JwtAuthenticationFilter(AccessTokenService accessTokenService, UserService userService) {
        this.accessTokenService = accessTokenService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length()), request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        AccessTokenClaims claims;
        try {
            claims = accessTokenService.parse(token);
        } catch (InvalidAccessTokenException e) {
            return;
        }

        Optional<User> user = userService.findById(claims.userId());
        if (user.isEmpty()) {
            return;
        }

        UserPrincipal principal = UserPrincipal.from(user.get());
        // Re-check status mới nhất từ DB (không tin claim trong token) — user
        // bị BLOCKED sau khi token đã phát hành vẫn phải bị từ chối ở đây.
        if (!principal.isEnabled()) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
