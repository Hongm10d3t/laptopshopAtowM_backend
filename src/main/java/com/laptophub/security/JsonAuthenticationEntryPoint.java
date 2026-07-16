package com.laptophub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.common.ApiResponse;
import com.laptophub.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

// Điểm duy nhất trả response cho mọi request chưa xác thực cần bị chặn (thiếu
// token, token hết hạn/sai chữ ký/sai issuer, user không tồn tại/BLOCKED —
// JwtAuthenticationFilter cố tình không tự viết response cho các trường hợp
// này, tất cả đều rơi vào đây) — đảm bảo response JSON đồng nhất theo
// ApiResponse thay vì mỗi nơi tự trả một hình dạng khác nhau.
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(ErrorCode.UNAUTHENTICATED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.error(
                ErrorCode.UNAUTHENTICATED.name(), ErrorCode.UNAUTHENTICATED.getDefaultMessage());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
