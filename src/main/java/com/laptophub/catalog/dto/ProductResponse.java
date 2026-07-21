package com.laptophub.catalog.dto;

import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductStatus;

import java.time.Instant;

// categoryName/brandName không nằm trên entity Product (FK dạng Long phẳng)
// nên phải truyền vào từ ngoài — caller (controller) tự tra tên qua
// CategoryService/BrandService cho 1 sản phẩm, không cần batch vì đây là
// response cho 1 item.
public record ProductResponse(
        Long id,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        String name,
        String slug,
        String shortDescription,
        String description,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductResponse from(Product product, String categoryName, String brandName) {
        return new ProductResponse(
                product.getId(),
                product.getCategoryId(),
                categoryName,
                product.getBrandId(),
                brandName,
                product.getName(),
                product.getSlug(),
                product.getShortDescription(),
                product.getDescription(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
