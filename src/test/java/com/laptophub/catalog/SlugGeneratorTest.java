package com.laptophub.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlugGeneratorTest {

    @Test
    void generate_lowercasesAndHyphenatesSpaces() {
        assertThat(SlugGenerator.generate("Laptop Gaming ASUS")).isEqualTo("laptop-gaming-asus");
    }

    @Test
    void generate_stripsVietnameseDiacritics() {
        assertThat(SlugGenerator.generate("Bàn phím cơ")).isEqualTo("ban-phim-co");
    }

    @Test
    void generate_replacesDVietnamese() {
        assertThat(SlugGenerator.generate("Đèn LED")).isEqualTo("den-led");
    }

    @Test
    void generate_collapsesConsecutiveSeparators() {
        assertThat(SlugGenerator.generate("Laptop -- Gaming!!  2024")).isEqualTo("laptop-gaming-2024");
    }

    @Test
    void generate_trimsLeadingAndTrailingHyphens() {
        assertThat(SlugGenerator.generate("--Laptop--")).isEqualTo("laptop");
    }

    @Test
    void generate_isIdempotentOnAlreadyValidSlug() {
        String slug = "laptop-gaming-asus-rog";
        assertThat(SlugGenerator.generate(slug)).isEqualTo(slug);
    }

    @Test
    void generate_rejectsNull() {
        assertThatThrownBy(() -> SlugGenerator.generate(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generate_rejectsBlank() {
        assertThatThrownBy(() -> SlugGenerator.generate("   ")).isInstanceOf(IllegalArgumentException.class);
    }
}
