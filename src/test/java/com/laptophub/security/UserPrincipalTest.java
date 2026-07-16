package com.laptophub.security;

import com.laptophub.user.User;
import com.laptophub.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void mapsAdminRoleToRoleAdminAuthority() {
        User user = User.create("admin@example.com", "hash", "Admin", null, UserRole.ADMIN);

        Collection<? extends GrantedAuthority> authorities = UserPrincipal.from(user).getAuthorities();

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    void mapsCustomerRoleToRoleCustomerAuthority() {
        User user = User.create("customer@example.com", "hash", "Customer", null, UserRole.CUSTOMER);

        Collection<? extends GrantedAuthority> authorities = UserPrincipal.from(user).getAuthorities();

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void activeStatusMapsToEnabled() {
        User user = User.create("active@example.com", "hash", "Active", null, UserRole.CUSTOMER);

        assertThat(UserPrincipal.from(user).isEnabled()).isTrue();
    }

    @Test
    void blockedStatusMapsToDisabled() {
        User user = User.create("blocked@example.com", "hash", "Blocked", null, UserRole.CUSTOMER);
        user.block();

        assertThat(UserPrincipal.from(user).isEnabled()).isFalse();
    }

    @Test
    void exposesEmailAsUsernameAndPasswordHashForAuthentication() {
        User user = User.create("id@example.com", "hashed-value", "Id", null, UserRole.CUSTOMER);

        UserPrincipal principal = UserPrincipal.from(user);

        assertThat(principal.getUsername()).isEqualTo("id@example.com");
        assertThat(principal.getPassword()).isEqualTo("hashed-value");
    }

    @Test
    void neverLeaksPasswordHashViaToString() {
        User user = User.create("tostring@example.com", "super-secret-hash", "Name", null, UserRole.CUSTOMER);

        String printed = UserPrincipal.from(user).toString();

        assertThat(printed).doesNotContain("super-secret-hash");
    }
}
