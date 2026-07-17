package com.laptophub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.user.UserService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Clock;

import static org.springframework.http.HttpMethod.POST;

/**
 * Security core chính thức dùng Bearer JWT qua JwtAuthenticationFilter, không
 * còn httpBasic/form login. Route rule: public health/public; public đúng 3
 * endpoint auth không cần token trước (register/login/refresh) — phần còn
 * lại của /auth/** (logout, logout-all, change-password) rơi vào
 * anyRequest().authenticated() vì cần xác thực theo hợp đồng; /admin/** yêu
 * cầu ROLE_ADMIN; /customer/** yêu cầu ROLE_CUSTOMER. Chưa xác thực ->
 * JsonAuthenticationEntryPoint (401); xác thực rồi nhưng sai role ->
 * JsonAccessDeniedHandler (403). CORS gắn qua CorsConfigurationSource (xem
 * CorsConfig) — không dùng WebMvcConfigurer song song để tránh 2 nơi tự định
 * nghĩa CORS xung đột nhau.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            AccessTokenService accessTokenService,
                                            UserService userService,
                                            AuthenticationEntryPoint jsonAuthenticationEntryPoint,
                                            AccessDeniedHandler jsonAccessDeniedHandler,
                                            CorsConfigurationSource corsConfigurationSource) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(accessTokenService, userService);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/public/**").permitAll()
                        .requestMatchers(POST, "/auth/register", "/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/customer/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint jsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new JsonAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public AccessDeniedHandler jsonAccessDeniedHandler(ObjectMapper objectMapper) {
        return new JsonAccessDeniedHandler(objectMapper);
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

    // Bean riêng thay vì gọi thẳng Clock.systemUTC() trong AccessTokenService —
    // để test có thể tiêm Clock.fixed(...) và không phụ thuộc thời gian hệ thống.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
