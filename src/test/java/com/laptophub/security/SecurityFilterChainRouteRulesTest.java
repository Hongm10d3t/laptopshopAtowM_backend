package com.laptophub.security;

import com.laptophub.user.entity.User;
import com.laptophub.user.repository.UserRepository;
import com.laptophub.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Xác nhận toàn bộ SecurityFilterChain do Spring lắp ráp thật (ASU-14):
// route rule theo role, không còn Basic challenge, và CORS không bị chặn
// bởi authentication. "/admin/..."/"/customer/..." không có controller thật
// nên qua được xác thực+phân quyền sẽ nhận 404 (route not found), không phải
// 401/403 — cách phân biệt "qua được" giống JwtAuthenticationFilterIntegrationTest.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityFilterChainRouteRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenService accessTokenService;

    private String tokenFor(UserRole role, String email) {
        User user = userRepository.saveAndFlush(
                User.create(email, passwordEncoder.encode("pw"), "Name", null, role));
        return accessTokenService.issue(UserPrincipal.from(user)).token();
    }

    @Test
    void publicEndpoint_staysAccessible() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void adminRoute_withAdminToken_passesAuthorization() throws Exception {
        String token = tokenFor(UserRole.ADMIN, "admin1@example.com");

        mockMvc.perform(get("/admin/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminRoute_withCustomerToken_returns403Json() throws Exception {
        String token = tokenFor(UserRole.CUSTOMER, "customer1@example.com");

        mockMvc.perform(get("/admin/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void adminRoute_withoutToken_returns401Json() throws Exception {
        mockMvc.perform(get("/admin/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void customerRoute_withCustomerToken_passesAuthorization() throws Exception {
        String token = tokenFor(UserRole.CUSTOMER, "customer2@example.com");

        mockMvc.perform(get("/customer/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerRoute_withAdminToken_returns403Json() throws Exception {
        String token = tokenFor(UserRole.ADMIN, "admin2@example.com");

        mockMvc.perform(get("/customer/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void unauthenticatedResponse_hasNoBasicAuthChallenge() throws Exception {
        mockMvc.perform(get("/admin/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void corsPreflight_toProtectedRoute_isNotBlockedByAuthentication() throws Exception {
        mockMvc.perform(options("/customer/anything")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}
