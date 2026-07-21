package com.laptophub.catalog.dto;

import java.math.BigDecimal;

// Không có status — kết quả public luôn ngầm định ACTIVE (đã lọc ở
// ProductRepository.searchPublic).
public record ProductListItemResponse(
        Long id,
        String name,
        String slug,
        String categoryName,
        String brandName,
        BigDecimal priceFrom,
        BigDecimal priceTo,
        String thumbnailUrl) {
}
