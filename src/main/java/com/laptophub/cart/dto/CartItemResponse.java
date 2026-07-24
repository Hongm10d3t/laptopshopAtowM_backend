package com.laptophub.cart.dto;

import com.laptophub.cart.service.CartLine;

import java.math.BigDecimal;

// unitPrice đọc live từ ProductVariant.price tại thời điểm gọi API (CartItem
// không lưu giá) — PROJECT_RULES.md §6.
public record CartItemResponse(
        Long id,
        Long productVariantId,
        String sku,
        String variantName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal) {

    public static CartItemResponse from(CartLine line) {
        BigDecimal unitPrice = line.variant().getPrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(line.item().getQuantity()));
        return new CartItemResponse(
                line.item().getId(),
                line.variant().getId(),
                line.variant().getSku(),
                line.variant().getVariantName(),
                unitPrice,
                line.item().getQuantity(),
                lineTotal);
    }
}
