package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySlug(String slug);

    // Admin listing — không lọc ACTIVE (Admin cần thấy cả sản phẩm INACTIVE
    // để bật lại). Mọi filter đều optional, theo pattern
    // "(:param IS NULL OR ...)" đã dùng ở UserRepository.search.
    @Query("""
            select p from Product p
            where (:categoryId is null or p.categoryId = :categoryId)
              and (:brandId is null or p.brandId = :brandId)
              and (:status is null or p.status = :status)
              and (:keyword is null or lower(p.name) like concat('%', :keyword, '%'))
            """)
    Page<Product> searchAdmin(Long categoryId, Long brandId, ProductStatus status, String keyword, Pageable pageable);

    // Public detail — chỉ trả về nếu product ACTIVE và category/brand của nó
    // cũng đang ACTIVE (category/brand có thể bị Admin deactivate sau khi
    // product đã tạo).
    @Query("""
            select p from Product p
            where p.slug = :slug and p.status = 'ACTIVE'
              and exists (select 1 from Category c where c.id = p.categoryId and c.status = 'ACTIVE')
              and exists (select 1 from Brand b where b.id = p.brandId and b.status = 'ACTIVE')
            """)
    Optional<Product> findPublicBySlug(String slug);

    // Public search — không dùng Pageable/Sort ở đây: kết quả trả về KHÔNG
    // phân trang, ProductSearchService tự sắp xếp (kể cả theo giá — không
    // phải field trực tiếp của Product) rồi tự phân trang thủ công trong bộ
    // nhớ. Chấp nhận được ở quy mô catalog MVP (xem ProductSearchService).
    @Query("""
            select p from Product p
            where p.status = 'ACTIVE'
              and exists (select 1 from Category c where c.id = p.categoryId and c.status = 'ACTIVE')
              and exists (select 1 from Brand b where b.id = p.brandId and b.status = 'ACTIVE')
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:brandId is null or p.brandId = :brandId)
              and (:keyword is null or lower(p.name) like concat('%', :keyword, '%'))
              and exists (
                  select 1 from ProductVariant v
                  where v.productId = p.id and v.status = 'ACTIVE'
                    and (:minPrice is null or v.price >= :minPrice)
                    and (:maxPrice is null or v.price <= :maxPrice)
              )
            """)
    List<Product> searchPublic(Long categoryId, Long brandId, String keyword, BigDecimal minPrice, BigDecimal maxPrice);
}
