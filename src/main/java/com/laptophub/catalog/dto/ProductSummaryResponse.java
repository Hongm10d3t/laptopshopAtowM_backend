package com.laptophub.catalog.dto;

import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductStatus;

import java.time.Instant;

// priceFrom/priceTo/thumbnailUrl sẽ bổ sung khi ProductVariant/ProductImage
// tồn tại (gói kế tiếp) — chưa có ở gói này.
public record ProductSummaryResponse(
        Long id,
        String name,
        String slug,
        String categoryName,
        String brandName,
        ProductStatus status,
        Instant createdAt) {

    public static ProductSummaryResponse from(Product product, String categoryName, String brandName) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                categoryName,
                brandName,
                product.getStatus(),
                product.getCreatedAt());
    }
}
