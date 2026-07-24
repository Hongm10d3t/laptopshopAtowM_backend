package com.laptophub.order.dto;

import com.laptophub.order.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productVariantId,
        String productName,
        String variantName,
        String sku,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal discountAmount,
        BigDecimal lineTotal) {

    public static OrderItemResponse from(OrderItem item) {
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                .subtract(item.getDiscountAmount());
        return new OrderItemResponse(
                item.getId(),
                item.getProductVariantId(),
                item.getProductName(),
                item.getVariantName(),
                item.getSku(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getDiscountAmount(),
                lineTotal);
    }
}
