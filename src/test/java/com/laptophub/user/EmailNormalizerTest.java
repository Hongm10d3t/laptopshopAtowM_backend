package com.laptophub.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailNormalizerTest {

    @Test
    void trimsAndLowercasesUsingLocaleRoot() {
        assertThat(EmailNormalizer.normalize(" User@Example.COM ")).isEqualTo("user@example.com");
    }

    @Test
    void isDeterministic() {
        String input = " Foo@Bar.COM ";
        assertThat(EmailNormalizer.normalize(input)).isEqualTo(EmailNormalizer.normalize(input));
    }

    @Test
    void doesNotChangeAlreadyNormalizedInput() {
        assertThat(EmailNormalizer.normalize("user@example.com")).isEqualTo("user@example.com");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> EmailNormalizer.normalize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmpty() {
        assertThatThrownBy(() -> EmailNormalizer.normalize(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> EmailNormalizer.normalize("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
