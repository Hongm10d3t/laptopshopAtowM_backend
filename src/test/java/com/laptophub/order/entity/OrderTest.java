package com.laptophub.order.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order newOrder() {
        return Order.create(1L, new BigDecimal("500.00"), "Giao giờ hành chính", "Nguyen Van A", "0900000000",
                "Ha Noi", "Cau Giay", "Dich Vong", "123 Duong ABC");
    }

    @Test
    void create_setsAllFields_andDefaultsStatusPendingAndPaymentMethodCod() {
        Order order = newOrder();

        assertThat(order.getUserId()).isEqualTo(1L);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("500.00");
        assertThat(order.getNote()).isEqualTo("Giao giờ hành chính");
        assertThat(order.getRecipientName()).isEqualTo("Nguyen Van A");
        assertThat(order.getPhone()).isEqualTo("0900000000");
        assertThat(order.getProvince()).isEqualTo("Ha Noi");
        assertThat(order.getDistrict()).isEqualTo("Cau Giay");
        assertThat(order.getWard()).isEqualTo("Dich Vong");
        assertThat(order.getStreetAddress()).isEqualTo("123 Duong ABC");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
    }

    @Test
    void create_allowsNullNote() {
        Order order = Order.create(1L, BigDecimal.TEN, null, "A", "0900000000", "HN", "CG", "DV", "123");

        assertThat(order.getNote()).isNull();
    }

    @Test
    void create_rejectsNullUserId() {
        assertThatThrownBy(() -> Order.create(null, BigDecimal.TEN, null, "A", "0900000000", "HN", "CG", "DV", "123"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullTotalAmount() {
        assertThatThrownBy(() -> Order.create(1L, null, null, "A", "0900000000", "HN", "CG", "DV", "123"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullRecipientName() {
        assertThatThrownBy(() -> Order.create(1L, BigDecimal.TEN, null, null, "0900000000", "HN", "CG", "DV", "123"))
                .isInstanceOf(NullPointerException.class);
    }
}
