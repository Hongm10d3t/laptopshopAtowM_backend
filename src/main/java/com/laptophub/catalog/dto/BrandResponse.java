package com.laptophub.catalog.dto;

import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.BrandStatus;

import java.time.Instant;

public record BrandResponse(
        Long id,
        String name,
        String slug,
        String description,
        String logoUrl,
        BrandStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static BrandResponse from(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.getDescription(),
                brand.getLogoUrl(),
                brand.getStatus(),
                brand.getCreatedAt(),
                brand.getUpdatedAt());
    }
}
