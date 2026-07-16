package com.laptophub.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigPasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

    @Test
    void matchesReturnsTrueForCorrectRawPassword() {
        String raw = "Sup3rSecret!";
        String encoded = passwordEncoder.encode(raw);

        assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
    }

    @Test
    void matchesReturnsFalseForWrongRawPassword() {
        String encoded = passwordEncoder.encode("Sup3rSecret!");

        assertThat(passwordEncoder.matches("wrong-password", encoded)).isFalse();
    }

    @Test
    void encodingSamePasswordTwiceProducesDifferentHashes() {
        String raw = "Sup3rSecret!";

        String first = passwordEncoder.encode(raw);
        String second = passwordEncoder.encode(raw);

        assertThat(first).isNotEqualTo(second);
        assertThat(passwordEncoder.matches(raw, first)).isTrue();
        assertThat(passwordEncoder.matches(raw, second)).isTrue();
    }

    @Test
    void encodedHashDoesNotContainRawPassword() {
        String raw = "Sup3rSecret!";

        String encoded = passwordEncoder.encode(raw);

        assertThat(encoded).doesNotContain(raw);
    }

    @Test
    void usesBCryptWithConfiguredStrength() {
        String encoded = passwordEncoder.encode("Sup3rSecret!");

        // Định dạng bcrypt chuẩn: $2a$/$2b$/$2y$<cost 2 chữ số>$<53 ký tự salt+hash>.
        // MD5 (32 hex) hay SHA-256 (64 hex) không bao giờ khớp pattern này.
        assertThat(encoded).matches("^\\$2[aby]\\$12\\$.{53}$");
    }
}
