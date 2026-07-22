package com.laptophub.inventory.entity;

import com.laptophub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

// Không có method sửa quantity — sửa dòng hàng khi phiếu còn DRAFT dùng
// full-replace ở StockReceiptService (xoá hết rồi tạo lại), giống
// ProductSpecValueService.upsertValues.
@Entity
@Table(name = "stock_receipt_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReceiptItem extends BaseEntity {

    @Column(name = "stock_receipt_id", nullable = false)
    private Long stockReceiptId;

    @Column(name = "product_variant_id", nullable = false)
    private Long productVariantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    private StockReceiptItem(Long stockReceiptId, Long productVariantId, Integer quantity) {
        this.stockReceiptId = Objects.requireNonNull(stockReceiptId, "stockReceiptId must not be null");
        this.productVariantId = Objects.requireNonNull(productVariantId, "productVariantId must not be null");
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
    }

    public static StockReceiptItem create(Long stockReceiptId, Long productVariantId, Integer quantity) {
        return new StockReceiptItem(stockReceiptId, productVariantId, quantity);
    }
}
