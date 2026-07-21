package com.laptophub.catalog.dto;

import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.entity.ProductVariantStatus;

import java.math.BigDecimal;

public record ProductVariantResponse(
        Long id,
        Long productId,
        String sku,
        String variantName,
        BigDecimal price,
        Integer ramGb,
        Integer storageGb,
        String storageType,
        String color,
        ProductVariantStatus status) {

    public static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getProductId(),
                variant.getSku(),
                variant.getVariantName(),
                variant.getPrice(),
                variant.getRamGb(),
                variant.getStorageGb(),
                variant.getStorageType(),
                variant.getColor(),
                variant.getStatus());
    }
}
