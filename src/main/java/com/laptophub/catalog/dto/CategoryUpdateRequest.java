package com.laptophub.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(

        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 150, message = "Tên danh mục tối đa 150 ký tự")
        String name,

        @Size(max = 160, message = "Slug tối đa 160 ký tự")
        String slug,

        @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
        String description) {
}
