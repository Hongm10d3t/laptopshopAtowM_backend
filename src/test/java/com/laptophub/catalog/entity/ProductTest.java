package com.laptophub.catalog.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void create_setsFieldsAndDefaultsToActive() {
        Product product = Product.create(1L, 2L, "Laptop Gaming ABC", "laptop-gaming-abc", "Mo ta ngan", "Mo ta dai");

        assertThat(product.getCategoryId()).isEqualTo(1L);
        assertThat(product.getBrandId()).isEqualTo(2L);
        assertThat(product.getName()).isEqualTo("Laptop Gaming ABC");
        assertThat(product.getSlug()).isEqualTo("laptop-gaming-abc");
        assertThat(product.getShortDescription()).isEqualTo("Mo ta ngan");
        assertThat(product.getDescription()).isEqualTo("Mo ta dai");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void create_rejectsNullCategoryId() {
        assertThatThrownBy(() -> Product.create(null, 2L, "Laptop", "laptop", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullBrandId() {
        assertThatThrownBy(() -> Product.create(1L, null, "Laptop", "laptop", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullName() {
        assertThatThrownBy(() -> Product.create(1L, 2L, null, "laptop", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void update_replacesEditableFields() {
        Product product = Product.create(1L, 2L, "Laptop", "laptop", null, null);

        product.update("Laptop Moi", "laptop-moi", "Ngan moi", "Dai moi");

        assertThat(product.getName()).isEqualTo("Laptop Moi");
        assertThat(product.getSlug()).isEqualTo("laptop-moi");
        assertThat(product.getShortDescription()).isEqualTo("Ngan moi");
        assertThat(product.getDescription()).isEqualTo("Dai moi");
    }

    @Test
    void changeCategory_and_changeBrand_updateIds() {
        Product product = Product.create(1L, 2L, "Laptop", "laptop", null, null);

        product.changeCategory(10L);
        product.changeBrand(20L);

        assertThat(product.getCategoryId()).isEqualTo(10L);
        assertThat(product.getBrandId()).isEqualTo(20L);
    }

    @Test
    void deactivate_and_activate_toggleStatus() {
        Product product = Product.create(1L, 2L, "Laptop", "laptop", null, null);

        product.deactivate();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);

        product.activate();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }
}
