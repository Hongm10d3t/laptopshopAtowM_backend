package com.laptophub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class JsonAuthenticationEntryPointTest {

    // findAndRegisterModules() để đăng ký jackson-datatype-jsr310 (serialize
    // Instant) — giống ObjectMapper bean thật Spring Boot auto-config, khác
    // với ObjectMapper trần mặc định không biết serialize java.time.Instant.
    private final JsonAuthenticationEntryPoint entryPoint =
            new JsonAuthenticationEntryPoint(new ObjectMapper().findAndRegisterModules());

    @Test
    void writesUnauthenticated401AsJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("bad creds"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        String body = response.getContentAsString();
        assertThat(body).contains("\"success\":false");
        assertThat(body).contains("\"errorCode\":\"UNAUTHENTICATED\"");
    }
}
