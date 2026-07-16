package com.laptophub.security;

import com.laptophub.user.User;
import com.laptophub.user.UserRepository;
import com.laptophub.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Xác nhận AuthenticationManager do Spring thật sự lắp ráp qua
// AuthenticationConfiguration (không phải wiring tay như
// AuthenticationProviderWiringTest) hoạt động đúng với DB thật.
// @Transactional để rollback dữ liệu test sau mỗi ca.
@SpringBootTest
@Transactional
class AuthenticationManagerIntegrationTest {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void authenticatesRealPersistedActiveUser() {
        String rawPassword = "correct-password";
        userRepository.saveAndFlush(
                User.create("engine@example.com", passwordEncoder.encode(rawPassword), "Engine", null, UserRole.CUSTOMER));

        Authentication result = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("engine@example.com", rawPassword));

        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    void rejectsWrongPasswordForRealPersistedUser() {
        userRepository.saveAndFlush(
                User.create("engine2@example.com", passwordEncoder.encode("correct-password"), "Engine", null,
                        UserRole.CUSTOMER));

        assertThatThrownBy(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("engine2@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
