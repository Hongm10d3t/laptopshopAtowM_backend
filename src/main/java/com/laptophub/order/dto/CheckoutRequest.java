package com.laptophub.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(

        @NotNull(message = "addressId không được để trống")
        Long addressId,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note) {
}
