package com.laptophub.order.entity;

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
// sửa/xoá — giống InventoryMovement. Thay thế audit-column-per-transition
// (kiểu StockReceipt.confirmedByUserId/confirmedAt) vì Order có tới 7
// transition, dồn hết vào 1 bảng lịch sử gọn hơn nhiều so với 14 cột rải
// trên orders.
@Entity
@Table(name = "order_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private OrderStatus toStatus;

    @Column(name = "changed_by_user_id", nullable = false)
    private Long changedByUserId;

    @Column(name = "note", length = 500)
    private String note;

    private OrderStatusHistory(Long orderId, OrderStatus fromStatus, OrderStatus toStatus, Long changedByUserId,
                                String note) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.fromStatus = Objects.requireNonNull(fromStatus, "fromStatus must not be null");
        this.toStatus = Objects.requireNonNull(toStatus, "toStatus must not be null");
        this.changedByUserId = Objects.requireNonNull(changedByUserId, "changedByUserId must not be null");
        this.note = note;
    }

    public static OrderStatusHistory create(Long orderId, OrderStatus fromStatus, OrderStatus toStatus,
                                             Long changedByUserId, String note) {
        return new OrderStatusHistory(orderId, fromStatus, toStatus, changedByUserId, note);
    }
}
