package com.laptophub.catalog.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductVariantTest {

    @Test
    void create_setsFieldsAndDefaultsToActive() {
        ProductVariant variant = ProductVariant.create(1L, "SKU-001", "16GB/512GB", new BigDecimal("999.99"),
                16, 512, "SSD", "Black");

        assertThat(variant.getProductId()).isEqualTo(1L);
        assertThat(variant.getSku()).isEqualTo("SKU-001");
        assertThat(variant.getVariantName()).isEqualTo("16GB/512GB");
        assertThat(variant.getPrice()).isEqualByComparingTo("999.99");
        assertThat(variant.getRamGb()).isEqualTo(16);
        assertThat(variant.getStorageGb()).isEqualTo(512);
        assertThat(variant.getStorageType()).isEqualTo("SSD");
        assertThat(variant.getColor()).isEqualTo("Black");
        assertThat(variant.getStatus()).isEqualTo(ProductVariantStatus.ACTIVE);
    }

    @Test
    void create_rejectsNullProductId() {
        assertThatThrownBy(() -> ProductVariant.create(null, "SKU-001", null, BigDecimal.TEN, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullSku() {
        assertThatThrownBy(() -> ProductVariant.create(1L, null, null, BigDecimal.TEN, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullPrice() {
        assertThatThrownBy(() -> ProductVariant.create(1L, "SKU-001", null, null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void update_replacesEditableFields_keepsSkuAndProductId() {
        ProductVariant variant = ProductVariant.create(1L, "SKU-001", null, BigDecimal.TEN, null, null, null, null);

        variant.update("8GB/256GB", new BigDecimal("799.00"), 8, 256, "SSD", "Silver");

        assertThat(variant.getSku()).isEqualTo("SKU-001");
        assertThat(variant.getProductId()).isEqualTo(1L);
        assertThat(variant.getVariantName()).isEqualTo("8GB/256GB");
        assertThat(variant.getPrice()).isEqualByComparingTo("799.00");
        assertThat(variant.getRamGb()).isEqualTo(8);
        assertThat(variant.getStorageGb()).isEqualTo(256);
        assertThat(variant.getStorageType()).isEqualTo("SSD");
        assertThat(variant.getColor()).isEqualTo("Silver");
    }

    @Test
    void deactivate_and_activate_toggleStatus() {
        ProductVariant variant = ProductVariant.create(1L, "SKU-001", null, BigDecimal.TEN, null, null, null, null);

        variant.deactivate();
        assertThat(variant.getStatus()).isEqualTo(ProductVariantStatus.INACTIVE);

        variant.activate();
        assertThat(variant.getStatus()).isEqualTo(ProductVariantStatus.ACTIVE);
    }
}
