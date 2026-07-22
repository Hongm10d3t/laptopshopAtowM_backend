package com.laptophub.inventory.dto;

import com.laptophub.inventory.entity.InventoryBalance;

public record InventoryBalanceResponse(
        Long productVariantId,
        String sku,
        Integer onHandQuantity,
        Integer reservedQuantity,
        Integer availableQuantity) {

    public static InventoryBalanceResponse from(InventoryBalance balance, String sku) {
        return new InventoryBalanceResponse(
                balance.getProductVariantId(),
                sku,
                balance.getOnHandQuantity(),
                balance.getReservedQuantity(),
                balance.getAvailableQuantity());
    }
}
