package com.laptophub.order.dto;

import com.laptophub.order.entity.Order;
import com.laptophub.order.entity.OrderStatus;
import com.laptophub.order.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

// Dùng cho GET /customer/orders (danh sách) — không kèm items để tránh N+1
// khi phân trang; xem chi tiết từng đơn qua GET /customer/orders/{id}
// (OrderResponse).
public record OrderSummaryResponse(
        Long id,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal totalAmount,
        Instant createdAt) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }
}
