package com.laptophub.cart.dto;

import com.laptophub.cart.service.CartLine;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, BigDecimal totalAmount) {

    public static CartResponse from(List<CartLine> lines) {
        List<CartItemResponse> items = lines.stream().map(CartItemResponse::from).toList();
        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, totalAmount);
    }
}
