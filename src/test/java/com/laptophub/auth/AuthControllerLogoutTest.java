package com.laptophub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.user.User;
import com.laptophub.user.UserRepository;
import com.laptophub.user.UserRole;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerLogoutTest {

    private static final Pattern REFRESH_COOKIE_VALUE = Pattern.compile("refreshToken=([^;]*)");

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

    private record LoginResultForTest(String accessToken, String refreshCookie) {
    }

    private LoginResultForTest login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        Matcher matcher = REFRESH_COOKIE_VALUE.matcher(result.getResponse().getHeader("Set-Cookie"));
        assertThat(matcher.find()).isTrue();
        return new LoginResultForTest(accessToken, matcher.group(1));
    }

    @Test
    void logout_withValidAccessToken_returns204_clearsCookie_andRevokesRefreshToken() throws Exception {
        userRepository.saveAndFlush(
                User.create("logout-ok@example.com", passwordEncoder.encode("Sup3rSecret!"), "Name", null,
                        UserRole.CUSTOMER));
        LoginResultForTest loginResult = login("logout-ok@example.com", "Sup3rSecret!");

        MvcResult result = mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + loginResult.accessToken()))
                .andExpect(status().isNoContent())
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).contains("refreshToken=").contains("Max-Age=0");

        User user = userRepository.findByEmail("logout-ok@example.com").orElseThrow();
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId());
        assertThat(active).isEmpty();
    }

    @Test
    void logout_withoutAccessToken_returns401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void logout_thenOldRefreshTokenCanNoLongerBeUsed() throws Exception {
        userRepository.saveAndFlush(
                User.create("logout-then-refresh@example.com", passwordEncoder.encode("Sup3rSecret!"), "Name", null,
                        UserRole.CUSTOMER));
        LoginResultForTest loginResult = login("logout-then-refresh@example.com", "Sup3rSecret!");

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + loginResult.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", loginResult.refreshCookie())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void logoutAll_withValidAccessToken_returns204_andRevokesSession() throws Exception {
        userRepository.saveAndFlush(
                User.create("logout-all-ok@example.com", passwordEncoder.encode("Sup3rSecret!"), "Name", null,
                        UserRole.CUSTOMER));
        LoginResultForTest loginResult = login("logout-all-ok@example.com", "Sup3rSecret!");

        mockMvc.perform(post("/auth/logout-all").header("Authorization", "Bearer " + loginResult.accessToken()))
                .andExpect(status().isNoContent());

        User user = userRepository.findByEmail("logout-all-ok@example.com").orElseThrow();
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())).isEmpty();
    }
}
