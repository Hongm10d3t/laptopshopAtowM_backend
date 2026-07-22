package com.laptophub.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StockReceiptCreateRequest(

        @NotBlank(message = "Mã phiếu nhập không được để trống")
        @Size(max = 50, message = "Mã phiếu nhập tối đa 50 ký tự")
        String code,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note,

        @NotEmpty(message = "Phiếu nhập phải có ít nhất 1 dòng hàng")
        @Valid
        List<StockReceiptItemRequest> items) {
}
