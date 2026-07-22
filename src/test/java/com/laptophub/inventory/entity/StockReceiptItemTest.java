package com.laptophub.inventory.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReceiptItemTest {

    @Test
    void create_setsFields() {
        StockReceiptItem item = StockReceiptItem.create(1L, 2L, 5);

        assertThat(item.getStockReceiptId()).isEqualTo(1L);
        assertThat(item.getProductVariantId()).isEqualTo(2L);
        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    void create_rejectsNullFields() {
        assertThatThrownBy(() -> StockReceiptItem.create(null, 2L, 5)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StockReceiptItem.create(1L, null, 5)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StockReceiptItem.create(1L, 2L, null)).isInstanceOf(NullPointerException.class);
    }
}
