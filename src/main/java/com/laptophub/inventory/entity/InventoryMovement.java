package com.laptophub.inventory.entity;

import com.laptophub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

// Bản ghi lịch sử immutable, append-only: chỉ có create(), không có method
// sửa/xoá — một dòng movement đã ghi không có lý do nghiệp vụ nào để thay đổi.
// referenceType/referenceId là tham chiếu polymorphic không FK (VD
// "STOCK_RECEIPT" cho RECEIPT, tương lai "ORDER_ITEM" cho
// RESERVE/RELEASE/SHIPMENT/RETURN) vì order_items chưa tồn tại — tránh module
// inventory phụ thuộc ngược vào module order chưa xây (PROJECT_RULES §2).
@Entity
@Table(name = "inventory_movements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryMovement extends BaseEntity {

    @Column(name = "product_variant_id", nullable = false)
    private Long productVariantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private InventoryMovementType type;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "on_hand_after", nullable = false)
    private Integer onHandAfter;

    @Column(name = "reserved_after", nullable = false)
    private Integer reservedAfter;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    private InventoryMovement(Long productVariantId, InventoryMovementType type, Integer quantity,
            Integer onHandAfter, Integer reservedAfter, String referenceType, Long referenceId, String reason,
            Long createdByUserId) {
        this.productVariantId = Objects.requireNonNull(productVariantId, "productVariantId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        this.onHandAfter = Objects.requireNonNull(onHandAfter, "onHandAfter must not be null");
        this.reservedAfter = Objects.requireNonNull(reservedAfter, "reservedAfter must not be null");
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reason = reason;
        this.createdByUserId = createdByUserId;
    }

    public static InventoryMovement create(Long productVariantId, InventoryMovementType type, Integer quantity,
            Integer onHandAfter, Integer reservedAfter, String referenceType, Long referenceId, String reason,
            Long createdByUserId) {
        return new InventoryMovement(productVariantId, type, quantity, onHandAfter, reservedAfter, referenceType,
                referenceId, reason, createdByUserId);
    }
}
