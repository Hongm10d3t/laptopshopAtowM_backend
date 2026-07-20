package com.laptophub.auth;

import com.laptophub.auth.token.RefreshTokenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class AuthConfig {
}
