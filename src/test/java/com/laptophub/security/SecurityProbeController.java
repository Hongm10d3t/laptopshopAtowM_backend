package com.laptophub.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Controller CHỈ tồn tại trong test source (src/test/java), không build vào
// jar production — mục đích duy nhất là có 3 route thật (1 cho mỗi role
// rule đã khai ở SecurityConfig) để test matrix gọi và nhận 200 thật kèm
// CurrentUser thật, thay vì suy luận gián tiếp qua 404 như các test trước.
@RestController
public class SecurityProbeController {

    private final CurrentUserProvider currentUserProvider;

    public SecurityProbeController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/probe/authenticated")
    public CurrentUser authenticated() {
        return currentUserProvider.getCurrentUser();
    }

    @GetMapping("/admin/probe")
    public CurrentUser admin() {
        return currentUserProvider.getCurrentUser();
    }

    @GetMapping("/customer/probe")
    public CurrentUser customer() {
        return currentUserProvider.getCurrentUser();
    }
}
