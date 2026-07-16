package com.laptophub.security;

// Cổng duy nhất để service nghiệp vụ (User, Cart, Order...) lấy identity
// đang đăng nhập — không tự đọc SecurityContextHolder hay tự cast Authentication
// ở nhiều nơi khác nhau. Không phụ thuộc HttpServletRequest/controller: chỉ
// cần SecurityContext đang có sẵn trên thread hiện tại (đúng với mọi request
// đã qua JwtAuthenticationFilter), nên gọi được từ bất kỳ tầng service nào.
public interface CurrentUserProvider {

    // Ném AppException(ErrorCode.UNAUTHENTICATED) nếu chưa có user thật đăng
    // nhập (kể cả anonymous) — chỉ nên xảy ra khi gọi nhầm từ luồng public.
    CurrentUser getCurrentUser();
}
