package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
