package com.laptophub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// Trước đây dùng WebMvcConfigurer.addCorsMappings — nhưng Spring Security
// chạy filter chain TRƯỚC DispatcherServlet/WebMvcConfigurer, nên với
// anyRequest().authenticated() đang bật, preflight OPTIONS có thể bị chặn
// 401 trước khi CORS ở tầng MVC kịp xử lý. Chuyển sang expose
// CorsConfigurationSource để SecurityConfig tự gắn vào HttpSecurity.cors(...)
// — chỉ một nguồn cấu hình CORS duy nhất, tránh xung đột 2 nơi tự định nghĩa.
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
