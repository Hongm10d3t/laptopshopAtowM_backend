package com.laptophub.inventory.dto;

import com.laptophub.inventory.entity.StockReceipt;
import com.laptophub.inventory.entity.StockReceiptStatus;

import java.time.Instant;

public record StockReceiptSummaryResponse(
        Long id,
        String code,
        StockReceiptStatus status,
        String note,
        Instant createdAt,
        Instant confirmedAt) {

    public static StockReceiptSummaryResponse from(StockReceipt receipt) {
        return new StockReceiptSummaryResponse(receipt.getId(), receipt.getCode(), receipt.getStatus(),
                receipt.getNote(), receipt.getCreatedAt(), receipt.getConfirmedAt());
    }
}
