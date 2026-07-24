package com.laptophub.cart.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    @Test
    void create_setsUserId() {
        Cart cart = Cart.create(1L);

        assertThat(cart.getUserId()).isEqualTo(1L);
    }

    @Test
    void create_rejectsNullUserId() {
        assertThatThrownBy(() -> Cart.create(null))
                .isInstanceOf(NullPointerException.class);
    }
}
