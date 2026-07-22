package com.laptophub.inventory.dto;

import com.laptophub.inventory.entity.StockReceiptItem;

public record StockReceiptItemResponse(
        Long id,
        Long productVariantId,
        String sku,
        Integer quantity) {

    public static StockReceiptItemResponse from(StockReceiptItem item, String sku) {
        return new StockReceiptItemResponse(item.getId(), item.getProductVariantId(), sku, item.getQuantity());
    }
}
