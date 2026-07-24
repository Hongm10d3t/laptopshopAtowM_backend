
package com.laptophub.inventory.repository;

import com.laptophub.inventory.entity.InventoryBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Các @Modifying @Query dưới đây là cơ chế duy nhất được dùng để thay đổi
// onHandQuantity/reservedQuantity (xem comment ở InventoryBalance) — mỗi
// UPDATE ... WHERE <điều kiện đủ tồn> là 1 statement atomic ở tầng InnoDB,
// trả về số dòng bị ảnh hưởng để Service phát hiện "không đủ tồn/không đủ
// chỗ giữ" (0 dòng) mà không cần optimistic lock.
public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {

        Optional<InventoryBalance> findByProductVariantId(Long productVariantId);

        // Dùng cho receiveStock/receiveReturn (delta dương) và adjust (delta +/-).
        // Điều kiện chặn cả on_hand âm lẫn vi phạm reserved <= on_hand.
        @Modifying(clearAutomatically = true)
        @Query("UPDATE InventoryBalance b SET b.onHandQuantity = b.onHandQuantity + :delta "
                        + "WHERE b.productVariantId = :productVariantId "
                        + "AND b.onHandQuantity + :delta >= 0 "
                        + "AND b.onHandQuantity + :delta >= b.reservedQuantity")
        int applyOnHandDelta(@Param("productVariantId") Long productVariantId, @Param("delta") int delta);

        // reserve: chỉ tăng reserved nếu available (on_hand - reserved) đủ.
        @Modifying(clearAutomatically = true)
        @Query("UPDATE InventoryBalance b SET b.reservedQuantity = b.reservedQuantity + :quantity "
                        + "WHERE b.productVariantId = :productVariantId "
                        + "AND b.onHandQuantity - b.reservedQuantity >= :quantity")
        int reserveQuantity(@Param("productVariantId") Long productVariantId, @Param("quantity") int quantity);

        // release: chỉ giảm reserved nếu đủ reserved để giảm.
        @Modifying(clearAutomatically = true)
        @Query("UPDATE InventoryBalance b SET b.reservedQuantity = b.reservedQuantity - :quantity "
                        + "WHERE b.productVariantId = :productVariantId AND b.reservedQuantity >= :quantity")
        int releaseQuantity(@Param("productVariantId") Long productVariantId, @Param("quantity") int quantity);

        // fulfill (xuất hàng): giảm cả on_hand và reserved cùng lúc, chỉ khi cả 2 đủ.
        @Modifying(clearAutomatically = true)
        @Query("UPDATE InventoryBalance b SET b.onHandQuantity = b.onHandQuantity - :quantity, "
                        + "b.reservedQuantity = b.reservedQuantity - :quantity "
                        + "WHERE b.productVariantId = :productVariantId "
                        + "AND b.onHandQuantity >= :quantity AND b.reservedQuantity >= :quantity")
        int fulfillQuantity(@Param("productVariantId") Long productVariantId, @Param("quantity") int quantity);
}
