package com.laptophub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.ChangePasswordRequest;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.auth.repository.RefreshTokenRepository;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerChangePasswordTest {

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
    void changePassword_withCorrectCurrentPassword_returns200_andRevokesRefreshToken() throws Exception {
        userRepository.saveAndFlush(
                User.create("change-pw-ok@example.com", passwordEncoder.encode("OldSecret1!"), "Name", null,
                        UserRole.CUSTOMER));
        LoginResultForTest loginResult = login("change-pw-ok@example.com", "OldSecret1!");

        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + loginResult.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("OldSecret1!", "NewSecret2!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User user = userRepository.findByEmail("change-pw-ok@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("NewSecret2!", user.getPasswordHash())).isTrue();
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())).isEmpty();
    }

    @Test
    void changePassword_withWrongCurrentPassword_returns401_andDoesNotChangePassword() throws Exception {
        userRepository.saveAndFlush(
                User.create("change-pw-wrong@example.com", passwordEncoder.encode("OldSecret1!"), "Name", null,
                        UserRole.CUSTOMER));
        LoginResultForTest loginResult = login("change-pw-wrong@example.com", "OldSecret1!");

        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + loginResult.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("WrongPassword!", "NewSecret2!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));

        User user = userRepository.findByEmail("change-pw-wrong@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("OldSecret1!", user.getPasswordHash())).isTrue();
    }

    @Test
    void changePassword_withoutAccessToken_returns401() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("Whatever1!", "NewSecret2!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void changePassword_withTooShortNewPassword_returns400() throws Exception {
        userRepository.saveAndFlush(
                User.create("change-pw-short@example.com", passwordEncoder.encode("OldSecret1!"), "Name", null,
                        UserRole.CUSTOMER));
        LoginResultForTest loginResult = login("change-pw-short@example.com", "OldSecret1!");

        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + loginResult.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("OldSecret1!", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void changePassword_thenOldRefreshTokenCanNoLongerBeUsed() throws Exception {
        userRepository.saveAndFlush(
                User.create("change-pw-then-refresh@example.com", passwordEncoder.encode("OldSecret1!"), "Name",
                        null, UserRole.CUSTOMER));
        LoginResultForTest loginResult = login("change-pw-then-refresh@example.com", "OldSecret1!");

        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + loginResult.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("OldSecret1!", "NewSecret2!"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", loginResult.refreshCookie())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }
}
