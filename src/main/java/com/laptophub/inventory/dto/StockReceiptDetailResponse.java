package com.laptophub.inventory.dto;

import com.laptophub.inventory.entity.StockReceipt;
import com.laptophub.inventory.entity.StockReceiptStatus;

import java.time.Instant;
import java.util.List;

public record StockReceiptDetailResponse(
        Long id,
        String code,
        StockReceiptStatus status,
        String note,
        List<StockReceiptItemResponse> items,
        Long createdByUserId,
        Instant createdAt,
        Long confirmedByUserId,
        Instant confirmedAt,
        Long cancelledByUserId,
        Instant cancelledAt) {

    public static StockReceiptDetailResponse from(StockReceipt receipt, List<StockReceiptItemResponse> items) {
        return new StockReceiptDetailResponse(
                receipt.getId(),
                receipt.getCode(),
                receipt.getStatus(),
                receipt.getNote(),
                items,
                receipt.getCreatedByUserId(),
                receipt.getCreatedAt(),
                receipt.getConfirmedByUserId(),
                receipt.getConfirmedAt(),
                receipt.getCancelledByUserId(),
                receipt.getCancelledAt());
    }
}
