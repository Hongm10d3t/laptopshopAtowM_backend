package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.entity.ProductVariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    List<ProductVariant> findByProductIdAndStatus(Long productId, ProductVariantStatus status);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

    // Batch min/max giá theo product — phục vụ priceFrom/priceTo trong danh
    // sách sản phẩm, tránh N+1 (1 query cho toàn bộ trang thay vì 1 query/sản
    // phẩm). Chỉ tính variant ACTIVE — variant đã ẩn không nên ảnh hưởng giá
    // hiển thị.
    @Query("""
            select v.productId as productId, min(v.price) as minPrice, max(v.price) as maxPrice
            from ProductVariant v
            where v.productId in :productIds and v.status = 'ACTIVE'
            group by v.productId
            """)
    List<ProductPriceRange> findMinMaxPriceByProductIdIn(List<Long> productIds);
}
