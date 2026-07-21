package com.laptophub.user;

import com.laptophub.common.ApiResponse;
import com.laptophub.security.CurrentUserProvider;
import com.laptophub.user.dto.ProfileResponse;
import com.laptophub.user.dto.UpdateProfileRequest;
import com.laptophub.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Quyền truy cập (hasRole('CUSTOMER') cho toàn bộ /customer/**) đã khai ở
// SecurityConfig — controller chỉ lo nhận request/gọi service, giống AuthController.
@RestController
@RequestMapping("/customer/profile")
public class CustomerProfileController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public CustomerProfileController(UserService userService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        Long userId = currentUserProvider.getCurrentUser().userId();
        var user = userService.findById(userId).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(ProfileResponse.from(user)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        var user = userService.updateProfile(userId, request.fullName(), request.phone());
        return ResponseEntity.ok(ApiResponse.success(ProfileResponse.from(user)));
    }
}
