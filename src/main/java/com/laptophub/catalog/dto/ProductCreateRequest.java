package com.laptophub.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(

        @NotNull(message = "Danh mục không được để trống")
        Long categoryId,

        @NotNull(message = "Thương hiệu không được để trống")
        Long brandId,

        @NotBlank(message = "Tên sản phẩm không được để trống")
        @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
        String name,

        @Size(max = 280, message = "Slug tối đa 280 ký tự")
        String slug,

        @Size(max = 500, message = "Mô tả ngắn tối đa 500 ký tự")
        String shortDescription,

        String description) {
}
