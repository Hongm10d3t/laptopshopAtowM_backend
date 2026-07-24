package com.laptophub.cart.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartItemTest {

    private CartItem newItem() {
        return CartItem.create(1L, 2L, 3);
    }

    @Test
    void create_setsAllFields() {
        CartItem item = newItem();

        assertThat(item.getCartId()).isEqualTo(1L);
        assertThat(item.getProductVariantId()).isEqualTo(2L);
        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    void create_rejectsNullCartId() {
        assertThatThrownBy(() -> CartItem.create(null, 2L, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullProductVariantId() {
        assertThatThrownBy(() -> CartItem.create(1L, null, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> CartItem.create(1L, 2L, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void increaseQuantity_addsToCurrentQuantity() {
        CartItem item = newItem();

        item.increaseQuantity(2);

        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    void increaseQuantity_rejectsNonPositiveDelta() {
        CartItem item = newItem();

        assertThatThrownBy(() -> item.increaseQuantity(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeQuantity_replacesQuantity() {
        CartItem item = newItem();

        item.changeQuantity(10);

        assertThat(item.getQuantity()).isEqualTo(10);
    }

    @Test
    void changeQuantity_rejectsNonPositiveQuantity() {
        CartItem item = newItem();

        assertThatThrownBy(() -> item.changeQuantity(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
