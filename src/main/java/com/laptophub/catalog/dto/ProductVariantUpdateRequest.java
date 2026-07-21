package com.laptophub.catalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Không có sku — đúng khả năng của ProductVariant.update() (SKU cố định
// sau khi tạo, không đổi qua đường sửa thông tin).
public record ProductVariantUpdateRequest(

        @Size(max = 255, message = "Tên phiên bản tối đa 255 ký tự")
        String variantName,

        @NotNull(message = "Giá không được để trống")
        @Positive(message = "Giá phải lớn hơn 0")
        BigDecimal price,

        @Positive(message = "RAM phải lớn hơn 0")
        Integer ramGb,

        @Positive(message = "Dung lượng lưu trữ phải lớn hơn 0")
        Integer storageGb,

        @Size(max = 20, message = "Loại lưu trữ tối đa 20 ký tự")
        String storageType,

        @Size(max = 50, message = "Màu sắc tối đa 50 ký tự")
        String color) {
}
