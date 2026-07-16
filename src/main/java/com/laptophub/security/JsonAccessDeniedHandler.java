package com.laptophub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.common.ApiResponse;
import com.laptophub.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

// Dùng cho request ĐÃ xác thực nhưng sai role (vd CUSTOMER gọi /admin/**) —
// khác JsonAuthenticationEntryPoint (dùng cho request CHƯA xác thực). 2 lớp
// tách riêng vì mã lỗi/HTTP status khác nhau: 401 UNAUTHENTICATED vs
// 403 UNAUTHORIZED, đúng API_CONVENTION.md.
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.error(
                ErrorCode.UNAUTHORIZED.name(), ErrorCode.UNAUTHORIZED.getDefaultMessage());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
