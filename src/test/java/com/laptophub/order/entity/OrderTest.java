package com.laptophub.order.entity;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.assertj.core.api.ThrowableAssert;
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

    private void assertInvalidOrderStatus(ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
    }

    @Test
    void confirm_transitionsPendingToConfirmed() {
        Order order = newOrder();

        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void confirm_rejectsWhenNotPending() {
        Order order = newOrder();
        order.confirm();

        assertInvalidOrderStatus(order::confirm);
    }

    @Test
    void prepare_transitionsConfirmedToPreparing() {
        Order order = newOrder();
        order.confirm();

        order.prepare();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void prepare_rejectsWhenNotConfirmed() {
        Order order = newOrder();

        assertInvalidOrderStatus(order::prepare);
    }

    @Test
    void ship_transitionsPreparingToShipping() {
        Order order = newOrder();
        order.confirm();
        order.prepare();

        order.ship();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    void ship_rejectsWhenNotPreparing() {
        Order order = newOrder();

        assertInvalidOrderStatus(order::ship);
    }

    @Test
    void deliver_transitionsShippingToDelivered() {
        Order order = newOrder();
        order.confirm();
        order.prepare();
        order.ship();

        order.deliver();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void deliver_rejectsWhenNotShipping() {
        Order order = newOrder();

        assertInvalidOrderStatus(order::deliver);
    }

    @Test
    void cancel_allowedFromPending() {
        Order order = newOrder();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_allowedFromConfirmed() {
        Order order = newOrder();
        order.confirm();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_allowedFromPreparing() {
        Order order = newOrder();
        order.confirm();
        order.prepare();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_rejectsWhenShipping() {
        Order order = newOrder();
        order.confirm();
        order.prepare();
        order.ship();

        assertInvalidOrderStatus(order::cancel);
    }

    @Test
    void requestReturn_transitionsDeliveredToReturnRequested() {
        Order order = newOrder();
        order.confirm();
        order.prepare();
        order.ship();
        order.deliver();

        order.requestReturn();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    @Test
    void requestReturn_rejectsWhenNotDelivered() {
        Order order = newOrder();

        assertInvalidOrderStatus(order::requestReturn);
    }

    @Test
    void approveReturn_transitionsReturnRequestedToReturned() {
        Order order = newOrder();
        order.confirm();
        order.prepare();
        order.ship();
        order.deliver();
        order.requestReturn();

        order.approveReturn();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURNED);
    }

    @Test
    void approveReturn_rejectsWhenNotReturnRequested() {
        Order order = newOrder();

        assertInvalidOrderStatus(order::approveReturn);
    }

    @Test
    void rejectReturn_transitionsReturnRequestedBackToDelivered() {
        Order order = newOrder();
        order.confirm();
        order.prepare();
        order.ship();
        order.deliver();
        order.requestReturn();

        order.rejectReturn();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void rejectReturn_rejectsWhenNotReturnRequested() {
        Order order = newOrder();

        assertInvalidOrderStatus(order::rejectReturn);
    }
}
