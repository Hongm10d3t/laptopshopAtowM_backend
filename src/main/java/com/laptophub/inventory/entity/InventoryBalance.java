package com.laptophub.inventory.entity;

import com.laptophub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

// Khác mọi entity còn lại của dự án: KHÔNG có domain method mutate (không
// increaseOnHand()/reserve()/...). Load-entity -> gọi method -> save() là
// read-modify-write không atomic ở tầng ứng dụng, sẽ tái tạo lại chính race
// condition (bán vượt tồn) mà thiết kế này cố tránh. Mọi thay đổi
// onHandQuantity/reservedQuantity PHẢI đi qua @Modifying @Query dạng
// "UPDATE ... WHERE <điều kiện đủ tồn>" ở InventoryBalanceRepository. create()
// chỉ dùng để khởi tạo dòng 0/0 lần đầu cho 1 productVariantId.
@Entity
@Table(name = "inventory_balances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryBalance extends BaseEntity {

    @Column(name = "product_variant_id", nullable = false, unique = true)
    private Long productVariantId;

    @Column(name = "on_hand_quantity", nullable = false)
    private Integer onHandQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    private InventoryBalance(Long productVariantId) {
        this.productVariantId = Objects.requireNonNull(productVariantId, "productVariantId must not be null");
        this.onHandQuantity = 0;
        this.reservedQuantity = 0;
    }

    public static InventoryBalance create(Long productVariantId) {
        return new InventoryBalance(productVariantId);
    }

    public int getAvailableQuantity() {
        return onHandQuantity - reservedQuantity;
    }
}
