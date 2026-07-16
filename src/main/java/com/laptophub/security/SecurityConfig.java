package com.laptophub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Khung tối thiểu: mở public health/auth/public, còn lại yêu cầu xác thực.
 * Chưa có JWT filter — sẽ thay httpBasic bằng JWT filter ở module auth.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/auth/**", "/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // strength 12, khác mặc định của BCryptPasswordEncoder (10): mặc định 10
    // được chọn từ nhiều năm trước cho phần cứng cũ hơn; 12 là baseline phổ
    // biến hiện nay, thời gian hash vẫn chấp nhận được (vài trăm ms) cho
    // register/login, không cần tune thêm ở MVP.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // hideUserNotFoundExceptions=true (giá trị mặc định, khai tường minh để
    // không ai vô tình đổi): email không tồn tại và sai mật khẩu phải cùng
    // ném BadCredentialsException, tránh lộ cho kẻ tấn công biết email nào
    // đã đăng ký (user enumeration). BLOCKED vẫn tách riêng thành
    // DisabledException vì DaoAuthenticationProvider kiểm tra isEnabled()
    // trước khi so khớp mật khẩu — đây là tín hiệu khác, không phải lộ email.
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                              PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    // Cách chuẩn của Spring Security 6 để expose AuthenticationManager bean
    // (WebSecurityConfigurerAdapter đã bị xóa) — lấy từ AuthenticationConfiguration,
    // nó sẽ dùng đúng DaoAuthenticationProvider bean ở trên.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
