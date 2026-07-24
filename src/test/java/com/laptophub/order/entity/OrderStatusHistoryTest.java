package com.laptophub.order.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusHistoryTest {

    @Test
    void create_setsAllFields() {
        OrderStatusHistory history = OrderStatusHistory.create(1L, OrderStatus.PENDING, OrderStatus.CONFIRMED, 99L,
                "Xác nhận đơn");

        assertThat(history.getOrderId()).isEqualTo(1L);
        assertThat(history.getFromStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(history.getChangedByUserId()).isEqualTo(99L);
        assertThat(history.getNote()).isEqualTo("Xác nhận đơn");
    }

    @Test
    void create_allowsNullNote() {
        OrderStatusHistory history = OrderStatusHistory.create(1L, OrderStatus.PENDING, OrderStatus.CANCELLED, 99L,
                null);

        assertThat(history.getNote()).isNull();
    }

    @Test
    void create_rejectsNullOrderId() {
        assertThatThrownBy(() -> OrderStatusHistory.create(null, OrderStatus.PENDING, OrderStatus.CONFIRMED, 99L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullFromStatus() {
        assertThatThrownBy(() -> OrderStatusHistory.create(1L, null, OrderStatus.CONFIRMED, 99L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullToStatus() {
        assertThatThrownBy(() -> OrderStatusHistory.create(1L, OrderStatus.PENDING, null, 99L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullChangedByUserId() {
        assertThatThrownBy(() -> OrderStatusHistory.create(1L, OrderStatus.PENDING, OrderStatus.CONFIRMED, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
