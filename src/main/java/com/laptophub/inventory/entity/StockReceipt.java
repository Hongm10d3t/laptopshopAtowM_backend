package com.laptophub.inventory.entity;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.entity.BaseEntity;
import com.laptophub.common.exception.AppException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

// Chỉ cho phép chuyển trạng thái từ DRAFT -> CONFIRMED hoặc DRAFT -> CANCELLED
// (không cancel được phiếu đã CONFIRMED — hàng đã lên kho thật, muốn đảo
// ngược phải qua InventoryService.adjust với lý do rõ ràng, không phải "hủy
// phiếu"). confirm()/cancel() không tự đụng tới inventory_balances — việc
// tăng on-hand khi confirm nằm ở StockReceiptService (gọi InventoryService).
@Entity
@Table(name = "stock_receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReceipt extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockReceiptStatus status;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "confirmed_by_user_id")
    private Long confirmedByUserId;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_by_user_id")
    private Long cancelledByUserId;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    private StockReceipt(String code, String note, Long createdByUserId) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.note = note;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId must not be null");
        this.status = StockReceiptStatus.DRAFT;
    }

    public static StockReceipt create(String code, String note, Long createdByUserId) {
        return new StockReceipt(code, note, createdByUserId);
    }

    public void confirm(Long confirmedByUserId, Instant confirmedAt) {
        if (this.status != StockReceiptStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_STOCK_RECEIPT_STATUS);
        }
        this.status = StockReceiptStatus.CONFIRMED;
        this.confirmedByUserId = Objects.requireNonNull(confirmedByUserId, "confirmedByUserId must not be null");
        this.confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
    }

    public void cancel(Long cancelledByUserId, Instant cancelledAt) {
        if (this.status != StockReceiptStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_STOCK_RECEIPT_STATUS);
        }
        this.status = StockReceiptStatus.CANCELLED;
        this.cancelledByUserId = Objects.requireNonNull(cancelledByUserId, "cancelledByUserId must not be null");
        this.cancelledAt = Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
    }
}
