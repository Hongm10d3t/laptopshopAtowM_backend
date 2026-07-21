package com.laptophub.catalog.dto;

import com.laptophub.catalog.entity.Brand;

public record BrandPublicResponse(Long id, String name, String slug, String logoUrl) {

    public static BrandPublicResponse from(Brand brand) {
        return new BrandPublicResponse(brand.getId(), brand.getName(), brand.getSlug(), brand.getLogoUrl());
    }
}
