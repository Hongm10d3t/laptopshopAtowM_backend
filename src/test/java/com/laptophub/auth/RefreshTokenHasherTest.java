package com.laptophub.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenHasherTest {

    // Vector chuẩn SHA-256("abc"), lấy từ `printf 'abc' | openssl dgst -sha256`
    // — không đoán giá trị, đối chiếu với công cụ độc lập bên ngoài JVM.
    @Test
    void hash_matchesKnownSha256TestVector() {
        assertThat(RefreshTokenHasher.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void hash_isDeterministic_sameInputAlwaysSameOutput() {
        String token = "some-refresh-token-value";

        assertThat(RefreshTokenHasher.hash(token)).isEqualTo(RefreshTokenHasher.hash(token));
    }

    @Test
    void hash_differsFromRawInput() {
        String token = "some-refresh-token-value";

        assertThat(RefreshTokenHasher.hash(token)).isNotEqualTo(token);
    }

    @Test
    void hash_isFixedLength64LowercaseHexChars() {
        String hash = RefreshTokenHasher.hash(RefreshTokenGenerator.generate());

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void hash_differentInputs_produceDifferentHashes() {
        String hashA = RefreshTokenHasher.hash("token-a");
        String hashB = RefreshTokenHasher.hash("token-b");

        assertThat(hashA).isNotEqualTo(hashB);
    }
}
