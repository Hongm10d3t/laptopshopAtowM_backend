package com.laptophub.security;

import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// Wiring tay (không qua Spring context) để test nhanh, đủ cho cả 4 tiêu chí
// nghiệm thu của ASU-10. AuthenticationManagerIntegrationTest verify riêng
// việc Spring thật sự lắp đúng bean này qua AuthenticationConfiguration.
@ExtendWith(MockitoExtension.class)
class AuthenticationProviderWiringTest {

    @Mock
    private UserService userService;

    private AuthenticationProvider authenticationProvider;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        SecurityConfig securityConfig = new SecurityConfig();
        passwordEncoder = securityConfig.passwordEncoder();
        CustomUserDetailsService userDetailsService = new CustomUserDetailsService(userService);
        authenticationProvider = securityConfig.authenticationProvider(userDetailsService, passwordEncoder);
    }

    @Test
    void activeUserWithCorrectPassword_authenticatesSuccessfully() {
        String rawPassword = "correct-password";
        User user = User.create("active@example.com", passwordEncoder.encode(rawPassword), "Active", null,
                UserRole.CUSTOMER);
        when(userService.findByNormalizedEmail("active@example.com")).thenReturn(Optional.of(user));

        Authentication result = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("active@example.com", rawPassword));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getName()).isEqualTo("active@example.com");
    }

    @Test
    void wrongPassword_failsWithBadCredentials() {
        User user = User.create("active2@example.com", passwordEncoder.encode("correct-password"), "Active", null,
                UserRole.CUSTOMER);
        when(userService.findByNormalizedEmail("active2@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("active2@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void unknownEmail_failsWithSameExceptionTypeAsWrongPassword() {
        when(userService.findByNormalizedEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("missing@example.com", "whatever")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void blockedUserWithCorrectPassword_failsWithDisabledException() {
        String rawPassword = "correct-password";
        User user = User.create("blocked@example.com", passwordEncoder.encode(rawPassword), "Blocked", null,
                UserRole.CUSTOMER);
        user.block();
        when(userService.findByNormalizedEmail("blocked@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("blocked@example.com", rawPassword)))
                .isInstanceOf(DisabledException.class);
    }
}
