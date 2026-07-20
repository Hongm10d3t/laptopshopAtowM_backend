package com.laptophub.user;

import com.laptophub.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    // PasswordEncoder thật (không mock) — cần hành vi encode thật để assert
    // hash khác raw password, giống cách RegisterServiceTest đã làm.
    private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

    @Captor
    private ArgumentCaptor<String> passwordHashCaptor;

    @Test
    void run_whenNoAdminExists_createsExactlyOneAdmin_withNormalizedEmail_andHashedPassword() {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, userService, passwordEncoder, " Admin@Example.COM ", "Sup3rSecret!");
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        User createdAdmin =
                User.create("admin@example.com", "hash-in-db", "Administrator", null, UserRole.ADMIN);
        createdAdmin.setId(1L);
        when(userService.createAdmin(eq("admin@example.com"), passwordHashCaptor.capture(), eq("Administrator")))
                .thenReturn(createdAdmin);

        runner.run(null);

        String capturedHash = passwordHashCaptor.getValue();
        assertThat(capturedHash).isNotEqualTo("Sup3rSecret!");
        assertThat(passwordEncoder.matches("Sup3rSecret!", capturedHash)).isTrue();
    }

    @Test
    void run_whenAdminAlreadyExists_doesNothing() {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, userService, passwordEncoder, "admin@example.com", "Sup3rSecret!");
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        runner.run(null);

        verifyNoInteractions(userService);
    }

    @Test
    void run_withPasswordTooShort_throwsIllegalStateException_andDoesNotCreateAdmin() {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, userService, passwordEncoder, "admin@example.com", "short1");
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> runner.run(null)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(userService);
    }

    @Test
    void run_withPasswordTooLong_throwsIllegalStateException() {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, userService, passwordEncoder, "admin@example.com", "a".repeat(73));
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> runner.run(null)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(userService);
    }
}
