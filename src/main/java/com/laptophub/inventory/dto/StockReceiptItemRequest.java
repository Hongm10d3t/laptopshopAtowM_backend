package com.laptophub.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockReceiptItemRequest(

        @NotNull(message = "Biến thể không được để trống")
        Long productVariantId,

        @NotNull(message = "Số lượng không được để trống")
        @Positive(message = "Số lượng phải lớn hơn 0")
        Integer quantity) {
}
