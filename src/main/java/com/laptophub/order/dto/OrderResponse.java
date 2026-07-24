package com.laptophub.order.dto;

import com.laptophub.order.entity.Order;
import com.laptophub.order.entity.OrderItem;
import com.laptophub.order.entity.OrderStatus;
import com.laptophub.order.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal totalAmount,
        String note,
        String recipientName,
        String phone,
        String province,
        String district,
        String ward,
        String streetAddress,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(Order order, List<OrderItem> items) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                order.getNote(),
                order.getRecipientName(),
                order.getPhone(),
                order.getProvince(),
                order.getDistrict(),
                order.getWard(),
                order.getStreetAddress(),
                items.stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
