package com.laptophub.security;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Spring Security mặc định set AnonymousAuthenticationToken (không
        // null) cho request chưa đăng nhập — principal của nó là String
        // "anonymousUser", không phải UserPrincipal, nên check instanceof
        // dưới đây tự động từ chối cả 2 trường hợp: chưa set Authentication
        // (null) lẫn anonymous, không cần check isAuthenticated() riêng.
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return new CurrentUser(principal.getId(), principal.getUsername(), principal.getRole());
    }
}
