package com.laptophub.catalog.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImageTest {

    @Test
    void create_setsFields() {
        ProductImage image = ProductImage.create(1L, "https://example.com/a.png", "Anh san pham", 0);

        assertThat(image.getProductId()).isEqualTo(1L);
        assertThat(image.getUrl()).isEqualTo("https://example.com/a.png");
        assertThat(image.getAltText()).isEqualTo("Anh san pham");
        assertThat(image.getSortOrder()).isZero();
    }

    @Test
    void create_rejectsNullProductId() {
        assertThatThrownBy(() -> ProductImage.create(null, "https://example.com/a.png", null, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullUrl() {
        assertThatThrownBy(() -> ProductImage.create(1L, null, null, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void changeSortOrder_updatesValue() {
        ProductImage image = ProductImage.create(1L, "https://example.com/a.png", null, 0);

        image.changeSortOrder(3);

        assertThat(image.getSortOrder()).isEqualTo(3);
    }

    @Test
    void changeAltText_updatesValue() {
        ProductImage image = ProductImage.create(1L, "https://example.com/a.png", null, 0);

        image.changeAltText("Anh moi");

        assertThat(image.getAltText()).isEqualTo("Anh moi");
    }
}
