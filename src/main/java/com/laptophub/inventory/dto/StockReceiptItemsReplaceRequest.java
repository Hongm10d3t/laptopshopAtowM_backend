package com.laptophub.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StockReceiptItemsReplaceRequest(

        @NotEmpty(message = "Phiếu nhập phải có ít nhất 1 dòng hàng")
        @Valid
        List<StockReceiptItemRequest> items) {
}
