package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.ProductSpecValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductSpecValueRepository extends JpaRepository<ProductSpecValue, Long> {

    List<ProductSpecValue> findByProductId(Long productId);

    // Phục vụ upsert kiểu "thay toàn bộ": xóa các giá trị cũ không còn nằm
    // trong danh sách specificationDefinitionId mới trước khi lưu/ghi đè
    // phần còn lại — clearAutomatically để tránh persistence context giữ
    // tham chiếu tới bản ghi đã xóa (cùng lý do RefreshTokenRepository).
    @Modifying(clearAutomatically = true)
    @Query("delete from ProductSpecValue v where v.productId = :productId and v.specificationDefinitionId not in :specificationDefinitionIds")
    void deleteByProductIdAndSpecificationDefinitionIdNotIn(Long productId, List<Long> specificationDefinitionIds);
}
