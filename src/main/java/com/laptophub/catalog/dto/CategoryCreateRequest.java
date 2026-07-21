package com.laptophub.catalog.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

// slug là tuỳ chọn — service luôn tự sinh/chuẩn hoá qua SlugGenerator dù
// Admin có nhập hay không (đúng PROJECT_RULES.md, tránh slug không hợp lệ).
public record CategoryCreateRequest(

        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 150, message = "Tên danh mục tối đa 150 ký tự")
        String name,

        @Size(max = 160, message = "Slug tối đa 160 ký tự")
        String slug,

        @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
        String description) {
}
