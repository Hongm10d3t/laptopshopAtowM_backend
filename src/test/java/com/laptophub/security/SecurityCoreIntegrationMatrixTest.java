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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Ma trận regression khóa hành vi Security core (ASU-09..14) trước khi thêm
// API Auth thật (register/login...). Không thêm production endpoint —
// SecurityProbeController chỉ tồn tại trong test source.
//
// Mỗi test tự tạo user với email riêng (không tái dùng dữ liệu giữa các
// test) và @Transactional rollback sau mỗi test method -> các test độc lập
// hoàn toàn, chạy đúng bất kể thứ tự JUnit chọn.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityCoreIntegrationMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private JwtProperties jwtProperties;

    private String issueToken(UserRole role, String email) {
        User user = userRepository.saveAndFlush(
                User.create(email, passwordEncoder.encode("pw"), "Name", null, role));
        return accessTokenService.issue(UserPrincipal.from(user)).token();
    }

    // Dùng chung JwtProperties (secret/issuer thật của app, autowired) nhưng
    // Clock cố định ở quá khứ -> token issue ra đã hết hạn ngay khi so với
    // Clock thật của filter lúc parse. Không cần chờ TTL thật trôi qua.
    private String issueExpiredToken(UserRole role, String email) {
        User user = userRepository.saveAndFlush(
                User.create(email, passwordEncoder.encode("pw"), "Name", null, role));
        Clock pastClock = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        AccessTokenService expiredIssuer = new AccessTokenService(jwtProperties, pastClock);
        return expiredIssuer.issue(UserPrincipal.from(user)).token();
    }

    // ---------- Anonymous ----------

    @Test
    void anonymous_publicEndpoint_isAccessible() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void anonymous_authenticatedEndpoint_returns401Json() throws Exception {
        mockMvc.perform(get("/probe/authenticated"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void anonymous_adminEndpoint_returns401Json() throws Exception {
        mockMvc.perform(get("/admin/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void anonymous_customerEndpoint_returns401Json() throws Exception {
        mockMvc.perform(get("/customer/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    // ---------- Valid CUSTOMER JWT ----------

    @Test
    void customerToken_authenticatedEndpoint_returnsCorrectIdentity() throws Exception {
        String token = issueToken(UserRole.CUSTOMER, "matrix-customer1@example.com");

        mockMvc.perform(get("/probe/authenticated").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("matrix-customer1@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void customerToken_customerEndpoint_isAllowed() throws Exception {
        String token = issueToken(UserRole.CUSTOMER, "matrix-customer2@example.com");

        mockMvc.perform(get("/customer/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void customerToken_adminEndpoint_returns403Json() throws Exception {
        String token = issueToken(UserRole.CUSTOMER, "matrix-customer3@example.com");

        mockMvc.perform(get("/admin/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    // ---------- Valid ADMIN JWT ----------

    @Test
    void adminToken_authenticatedEndpoint_returnsCorrectIdentity() throws Exception {
        String token = issueToken(UserRole.ADMIN, "matrix-admin1@example.com");

        mockMvc.perform(get("/probe/authenticated").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("matrix-admin1@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void adminToken_adminEndpoint_isAllowed() throws Exception {
        String token = issueToken(UserRole.ADMIN, "matrix-admin2@example.com");

        mockMvc.perform(get("/admin/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void adminToken_customerEndpoint_returns403Json() throws Exception {
        String token = issueToken(UserRole.ADMIN, "matrix-admin3@example.com");

        mockMvc.perform(get("/customer/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    // ---------- Malformed / expired / blocked ----------

    @Test
    void malformedToken_returns401Json() throws Exception {
        mockMvc.perform(get("/probe/authenticated").header("Authorization", "Bearer this-is-not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void expiredToken_returns401Json() throws Exception {
        String token = issueExpiredToken(UserRole.CUSTOMER, "matrix-expired@example.com");

        mockMvc.perform(get("/probe/authenticated").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void blockedUser_withStillValidToken_returns401Json() throws Exception {
        User user = userRepository.saveAndFlush(User.create(
                "matrix-blocked@example.com", passwordEncoder.encode("pw"), "Name", null, UserRole.CUSTOMER));
        String token = accessTokenService.issue(UserPrincipal.from(user)).token();

        // Block SAU khi đã phát token — JWT vẫn còn hạn dùng, chữ ký vẫn hợp
        // lệ, chỉ có status trong DB đã đổi. Đúng tiêu chí "Blocked user bị
        // từ chối dù JWT còn hạn".
        user.block();
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/customer/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }
}
