package com.laptophub.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryAdjustRequest(

        @NotNull(message = "Số lượng điều chỉnh không được để trống")
        Integer delta,

        @NotBlank(message = "Lý do điều chỉnh không được để trống")
        @Size(max = 255, message = "Lý do điều chỉnh tối đa 255 ký tự")
        String reason) {
}
