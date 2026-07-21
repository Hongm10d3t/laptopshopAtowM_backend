package com.laptophub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.auth.entity.RefreshToken;
import com.laptophub.auth.repository.RefreshTokenRepository;
import com.laptophub.user.entity.User;
import com.laptophub.user.repository.UserRepository;
import com.laptophub.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User registerUser(String email, String rawPassword, UserRole role) {
        User user = userRepository.saveAndFlush(
                User.create(email, passwordEncoder.encode(rawPassword), "Name", null, role));
        return user;
    }

    @Test
    void login_validCredentials_returns200_setsCookie_andPersistsOneActiveRefreshToken() throws Exception {
        registerUser("login-ok@example.com", "Sup3rSecret!", UserRole.CUSTOMER);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("login-ok@example.com", "Sup3rSecret!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).contains("refreshToken=")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax")
                .contains("Path=/api/v1/auth");

        // Không lộ refresh token trong JSON body.
        assertThat(result.getResponse().getContentAsString()).doesNotContain("refreshToken");

        User user = userRepository.findByEmail("login-ok@example.com").orElseThrow();
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId());
        assertThat(active).hasSize(1);
    }

    @Test
    void login_wrongPassword_returns401InvalidCredentials() throws Exception {
        registerUser("login-wrongpw@example.com", "Sup3rSecret!", UserRole.CUSTOMER);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("login-wrongpw@example.com", "totally-wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_unknownEmail_returns401_sameErrorCodeAsWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("no-such-user@example.com", "whatever"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_blockedAccount_returns403AccountBlocked() throws Exception {
        User user = registerUser("login-blocked@example.com", "Sup3rSecret!", UserRole.CUSTOMER);
        user.block();
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("login-blocked@example.com", "Sup3rSecret!"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_BLOCKED"));
    }

    @Test
    void login_secondTime_revokesFirstSession_keepsExactlyOneActiveToken() throws Exception {
        registerUser("login-twice@example.com", "Sup3rSecret!", UserRole.CUSTOMER);
        LoginRequest request = new LoginRequest("login-twice@example.com", "Sup3rSecret!");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("login-twice@example.com").orElseThrow();
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId());
        assertThat(active).hasSize(1);
    }
}
