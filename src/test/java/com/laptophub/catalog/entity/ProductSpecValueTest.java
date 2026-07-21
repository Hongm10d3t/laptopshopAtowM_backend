package com.laptophub.catalog.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductSpecValueTest {

    @Test
    void create_setsFields() {
        ProductSpecValue value = ProductSpecValue.create(1L, 2L, "Intel Core i7-13700H");

        assertThat(value.getProductId()).isEqualTo(1L);
        assertThat(value.getSpecificationDefinitionId()).isEqualTo(2L);
        assertThat(value.getValue()).isEqualTo("Intel Core i7-13700H");
    }

    @Test
    void create_rejectsNullProductId() {
        assertThatThrownBy(() -> ProductSpecValue.create(null, 2L, "value"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullSpecificationDefinitionId() {
        assertThatThrownBy(() -> ProductSpecValue.create(1L, null, "value"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullValue() {
        assertThatThrownBy(() -> ProductSpecValue.create(1L, 2L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void changeValue_updatesValue() {
        ProductSpecValue value = ProductSpecValue.create(1L, 2L, "16GB");

        value.changeValue("32GB");

        assertThat(value.getValue()).isEqualTo("32GB");
    }
}
