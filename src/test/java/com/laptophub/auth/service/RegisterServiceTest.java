package com.laptophub.auth.service;

import com.laptophub.auth.dto.RegisterRequest;
import com.laptophub.auth.dto.RegisterResponse;
import com.laptophub.security.SecurityConfig;
import com.laptophub.user.User;
import com.laptophub.user.UserRole;
import com.laptophub.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private UserService userService;

    // PasswordEncoder thật (không mock) — cần hành vi encode/matches thật
    // để assert hash khác raw password và match ngược lại đúng.
    private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

    @Captor
    private ArgumentCaptor<String> passwordHashCaptor;

    @Test
    void register_normalizesEmail_hashesPassword_andReturnsSafeResponse() {
        RegisterService registerService = new RegisterService(userService, passwordEncoder);
        RegisterRequest request =
                new RegisterRequest(" User@Example.COM ", "Sup3rSecret!", "Nguyen Van A", "0901234567");

        User createdUser = User.create(
                "user@example.com", "hash-actually-stored-in-db", "Nguyen Van A", "0901234567", UserRole.CUSTOMER);
        createdUser.setId(1L);
        when(userService.createCustomer(
                eq("user@example.com"), passwordHashCaptor.capture(), eq("Nguyen Van A"), eq("0901234567")))
                .thenReturn(createdUser);

        RegisterResponse response = registerService.register(request);

        // Response an toàn, đúng dữ liệu đã tạo.
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.fullName()).isEqualTo("Nguyen Van A");
        assertThat(response.phone()).isEqualTo("0901234567");

        // Giá trị thực sự đưa vào UserService không phải raw password.
        String capturedHash = passwordHashCaptor.getValue();
        assertThat(capturedHash).isNotEqualTo("Sup3rSecret!");
        assertThat(passwordEncoder.matches("Sup3rSecret!", capturedHash)).isTrue();
    }
}
