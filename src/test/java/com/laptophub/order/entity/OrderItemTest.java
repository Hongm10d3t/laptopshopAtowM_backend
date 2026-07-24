package com.laptophub.order.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    private OrderItem newItem() {
        return OrderItem.create(1L, 2L, "Laptop ABC", "16GB/512GB", "SKU-001", new BigDecimal("1000.00"), 2);
    }

    @Test
    void create_setsAllFields_andDefaultsDiscountAmountZero() {
        OrderItem item = newItem();

        assertThat(item.getOrderId()).isEqualTo(1L);
        assertThat(item.getProductVariantId()).isEqualTo(2L);
        assertThat(item.getProductName()).isEqualTo("Laptop ABC");
        assertThat(item.getVariantName()).isEqualTo("16GB/512GB");
        assertThat(item.getSku()).isEqualTo("SKU-001");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("1000.00");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void create_allowsNullVariantName() {
        OrderItem item = OrderItem.create(1L, 2L, "Laptop ABC", null, "SKU-001", BigDecimal.TEN, 1);

        assertThat(item.getVariantName()).isNull();
    }

    @Test
    void create_rejectsNullOrderId() {
        assertThatThrownBy(() -> OrderItem.create(null, 2L, "Laptop ABC", null, "SKU-001", BigDecimal.TEN, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullProductVariantId() {
        assertThatThrownBy(() -> OrderItem.create(1L, null, "Laptop ABC", null, "SKU-001", BigDecimal.TEN, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> OrderItem.create(1L, 2L, "Laptop ABC", null, "SKU-001", BigDecimal.TEN, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
