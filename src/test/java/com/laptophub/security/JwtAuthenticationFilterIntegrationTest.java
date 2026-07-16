package com.laptophub.security;

import com.laptophub.user.User;
import com.laptophub.user.UserRepository;
import com.laptophub.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Xác nhận toàn bộ dây chuyền thật (SecurityFilterChain + JwtAuthenticationFilter
// + JsonAuthenticationEntryPoint) do Spring lắp ráp, không phải wiring tay.
// "/some-protected-resource" không có controller nào ánh xạ tới — nếu xác
// thực thành công thì request đi tiếp tới MVC và nhận 404 (không tìm thấy
// route); nếu xác thực thất bại thì bị chặn ở tầng Security và nhận 401 từ
// JsonAuthenticationEntryPoint. 2 mã trạng thái này phân biệt rõ ràng "qua
// được xác thực" và "bị chặn ở xác thực".
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtAuthenticationFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenService accessTokenService;

    @Test
    void publicEndpoint_noToken_staysAccessible() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_noToken_returns401Json() throws Exception {
        mockMvc.perform(get("/some-protected-resource"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void protectedEndpoint_invalidToken_returns401Json() throws Exception {
        mockMvc.perform(get("/some-protected-resource").header("Authorization", "Bearer garbage-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void protectedEndpoint_validTokenForActiveUser_passesAuthenticationLayer() throws Exception {
        User user = userRepository.saveAndFlush(
                User.create("filter-active@example.com", passwordEncoder.encode("pw"), "Active", null, UserRole.CUSTOMER));
        AccessToken token = accessTokenService.issue(UserPrincipal.from(user));

        mockMvc.perform(get("/some-protected-resource").header("Authorization", "Bearer " + token.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpoint_userBlockedAfterTokenIssued_isRejected() throws Exception {
        User user = userRepository.saveAndFlush(
                User.create("filter-blocked@example.com", passwordEncoder.encode("pw"), "Name", null, UserRole.CUSTOMER));
        AccessToken token = accessTokenService.issue(UserPrincipal.from(user));

        // Block SAU khi đã phát token — mô phỏng Admin khóa tài khoản trong
        // lúc access token cũ vẫn còn hạn dùng.
        user.block();
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/some-protected-resource").header("Authorization", "Bearer " + token.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }
}
