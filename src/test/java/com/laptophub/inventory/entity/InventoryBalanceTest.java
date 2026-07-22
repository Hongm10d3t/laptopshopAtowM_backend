package com.laptophub.inventory.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryBalanceTest {

    @Test
    void create_initializesZeroOnHandAndReserved() {
        InventoryBalance balance = InventoryBalance.create(1L);

        assertThat(balance.getProductVariantId()).isEqualTo(1L);
        assertThat(balance.getOnHandQuantity()).isZero();
        assertThat(balance.getReservedQuantity()).isZero();
        assertThat(balance.getAvailableQuantity()).isZero();
    }

    @Test
    void create_rejectsNullProductVariantId() {
        assertThatThrownBy(() -> InventoryBalance.create(null)).isInstanceOf(NullPointerException.class);
    }
}
