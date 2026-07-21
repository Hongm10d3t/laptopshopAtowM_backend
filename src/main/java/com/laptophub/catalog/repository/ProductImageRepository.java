package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    // Batch cho danh sách sản phẩm (public/admin listing) — tránh N+1 khi
    // cần ảnh đại diện của nhiều sản phẩm cùng lúc.
    List<ProductImage> findByProductIdInOrderBySortOrderAsc(List<Long> productIds);

    Optional<ProductImage> findByIdAndProductId(Long id, Long productId);
}
