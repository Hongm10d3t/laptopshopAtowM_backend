package com.laptophub.catalog.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrandTest {

    @Test
    void create_setsFieldsAndDefaultsToActive() {
        Brand brand = Brand.create("Asus", "asus", "Thuong hieu Asus", "https://example.com/asus-logo.png");

        assertThat(brand.getName()).isEqualTo("Asus");
        assertThat(brand.getSlug()).isEqualTo("asus");
        assertThat(brand.getDescription()).isEqualTo("Thuong hieu Asus");
        assertThat(brand.getLogoUrl()).isEqualTo("https://example.com/asus-logo.png");
        assertThat(brand.getStatus()).isEqualTo(BrandStatus.ACTIVE);
    }

    @Test
    void create_rejectsNullName() {
        assertThatThrownBy(() -> Brand.create(null, "asus", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullSlug() {
        assertThatThrownBy(() -> Brand.create("Asus", null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void update_replacesFields() {
        Brand brand = Brand.create("Asus", "asus", null, null);

        brand.update("Dell", "dell", "Thuong hieu Dell", "https://example.com/dell-logo.png");

        assertThat(brand.getName()).isEqualTo("Dell");
        assertThat(brand.getSlug()).isEqualTo("dell");
        assertThat(brand.getDescription()).isEqualTo("Thuong hieu Dell");
        assertThat(brand.getLogoUrl()).isEqualTo("https://example.com/dell-logo.png");
    }

    @Test
    void deactivate_and_activate_toggleStatus() {
        Brand brand = Brand.create("Asus", "asus", null, null);

        brand.deactivate();
        assertThat(brand.getStatus()).isEqualTo(BrandStatus.INACTIVE);

        brand.activate();
        assertThat(brand.getStatus()).isEqualTo(BrandStatus.ACTIVE);
    }
}
