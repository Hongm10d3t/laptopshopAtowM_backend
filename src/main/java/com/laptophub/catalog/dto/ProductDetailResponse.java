package com.laptophub.catalog.dto;

import java.util.List;

// variants/images chỉ gồm phần tử ACTIVE (đã lọc ở ProductSearchService) —
// Guest/Customer không cần thấy variant/ảnh Admin đã ẩn.
public record ProductDetailResponse(
        Long id,
        String name,
        String slug,
        String categoryName,
        String brandName,
        String shortDescription,
        String description,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants,
        List<ProductSpecValueResponse> specifications) {
}
