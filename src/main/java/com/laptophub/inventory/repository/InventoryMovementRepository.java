package com.laptophub.inventory.repository;

import com.laptophub.inventory.entity.InventoryMovement;
import com.laptophub.inventory.entity.InventoryMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    Page<InventoryMovement> findByProductVariantIdOrderByCreatedAtDesc(Long productVariantId, Pageable pageable);

    @Query("SELECT m FROM InventoryMovement m WHERE m.productVariantId = :productVariantId "
            + "AND (:type IS NULL OR m.type = :type) ORDER BY m.createdAt DESC")
    Page<InventoryMovement> findByProductVariantIdAndOptionalType(@Param("productVariantId") Long productVariantId,
            @Param("type") InventoryMovementType type, Pageable pageable);
}
