package com.laptophub.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandCreateRequest(

        @NotBlank(message = "Tên thương hiệu không được để trống")
        @Size(max = 150, message = "Tên thương hiệu tối đa 150 ký tự")
        String name,

        @Size(max = 160, message = "Slug tối đa 160 ký tự")
        String slug,

        @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
        String description,

        @Size(max = 500, message = "Logo URL tối đa 500 ký tự")
        String logoUrl) {
}
