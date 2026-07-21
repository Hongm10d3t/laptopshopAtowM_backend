package com.laptophub.user;

import com.laptophub.common.ApiResponse;
import com.laptophub.common.dto.PageResponse;
import com.laptophub.common.exception.AppException;
import com.laptophub.common.ErrorCode;
import com.laptophub.security.CurrentUserProvider;
import com.laptophub.user.dto.AdminUserResponse;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.entity.UserStatus;
import com.laptophub.user.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Quyền truy cập (hasRole('ADMIN') cho /admin/**) đã khai ở SecurityConfig.
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public AdminUserController(UserService userService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> list(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
        var page = userService.search(role, status, normalizedKeyword, pageable).map(AdminUserResponse::from);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getOne(@PathVariable Long id) {
        var user = userService.findById(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.success(AdminUserResponse.from(user)));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponse<AdminUserResponse>> block(@PathVariable Long id) {
        Long actingAdminId = currentUserProvider.getCurrentUser().userId();
        var user = userService.blockUser(actingAdminId, id);
        return ResponseEntity.ok(ApiResponse.success(AdminUserResponse.from(user)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> activate(@PathVariable Long id) {
        var user = userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success(AdminUserResponse.from(user)));
    }
}
