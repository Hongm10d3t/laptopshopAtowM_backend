package com.laptophub.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// sortOrder để trống -> ảnh được thêm vào cuối danh sách (xử lý ở
// ProductImageService), không bắt buộc Admin phải tự tính thứ tự.
public record ProductImageCreateRequest(

        @NotBlank(message = "URL ảnh không được để trống")
        @Size(max = 500, message = "URL ảnh tối đa 500 ký tự")
        String url,

        @Size(max = 255, message = "Alt text tối đa 255 ký tự")
        String altText,

        Integer sortOrder) {
}
