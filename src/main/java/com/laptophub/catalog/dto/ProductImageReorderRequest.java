package com.laptophub.catalog.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// Toàn bộ thứ tự mong muốn — vị trí trong danh sách chính là sort_order mới
// (index 0 = đầu tiên), không truyền số thứ tự riêng cho từng ảnh.
public record ProductImageReorderRequest(

        @NotEmpty(message = "Danh sách ảnh không được để trống")
        List<Long> orderedImageIds) {
}
