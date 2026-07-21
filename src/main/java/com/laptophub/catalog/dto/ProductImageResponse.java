package com.laptophub.catalog.dto;

import com.laptophub.catalog.entity.ProductImage;

public record ProductImageResponse(Long id, String url, String altText, int sortOrder) {

    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(image.getId(), image.getUrl(), image.getAltText(), image.getSortOrder());
    }
}
