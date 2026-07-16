package com.laptophub.security;

import com.laptophub.user.User;
import com.laptophub.user.UserRole;
import com.laptophub.user.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// passwordHash chỉ phục vụ DaoAuthenticationProvider so khớp lúc đăng nhập —
// không bao giờ log, serialize ra JSON hay đưa vào response API.
// Không giữ tham chiếu tới entity User (tránh lazy-loading ngoài transaction
// và tránh entity JPA sống trong SecurityContext) — chỉ copy field cần thiết.
public final class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final boolean enabled;

    private UserPrincipal(Long id, String email, String passwordHash, UserRole role, boolean enabled) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
    }

    public static UserPrincipal from(User user) {
        // ACTIVE -> enabled, BLOCKED -> disabled. Admin khóa/mở tài khoản là
        // hành động rõ ràng (không phải khóa tạm do đăng nhập sai nhiều lần)
        // nên map vào enabled/disabled thay vì accountNonLocked.
        boolean enabled = user.getStatus() == UserStatus.ACTIVE;
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole(), enabled);
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
