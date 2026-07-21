package com.laptophub.security;

import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserService userService;

    @Test
    void loadUserByUsername_normalizesEmailBeforeLookup() {
        User user = User.create("user@example.com", "hash", "Name", null, UserRole.CUSTOMER);
        when(userService.findByNormalizedEmail("user@example.com")).thenReturn(Optional.of(user));

        UserDetails result = new CustomUserDetailsService(userService).loadUserByUsername(" User@Example.COM ");

        assertThat(result.getUsername()).isEqualTo("user@example.com");
        verify(userService).findByNormalizedEmail("user@example.com");
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFoundException_whenEmailDoesNotExist() {
        when(userService.findByNormalizedEmail("missing@example.com")).thenReturn(Optional.empty());

        CustomUserDetailsService service = new CustomUserDetailsService(userService);

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
