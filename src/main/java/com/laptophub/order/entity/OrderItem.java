package com.laptophub.order.entity;

import com.laptophub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

// orderId/productVariantId là FK dạng Long phẳng, đúng tiền lệ chung của dự
// án. productName/variantName/sku/unitPrice là snapshot tại thời điểm mua
// (DATABASE_DESIGN.md §2 "Order") — không đọc lại từ ProductVariant sau này.
// discountAmount luôn 0 ở Giai đoạn 5 (chưa có voucher, Giai đoạn 7) —
// hardcode trong constructor, không nhận tham số.
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_variant_id", nullable = false)
    private Long productVariantId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "variant_name", length = 255)
    private String variantName;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    private OrderItem(Long orderId, Long productVariantId, String productName, String variantName, String sku,
                       BigDecimal unitPrice, int quantity) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.productVariantId = Objects.requireNonNull(productVariantId, "productVariantId must not be null");
        this.productName = Objects.requireNonNull(productName, "productName must not be null");
        this.variantName = variantName;
        this.sku = Objects.requireNonNull(sku, "sku must not be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.quantity = quantity;
        this.discountAmount = BigDecimal.ZERO;
    }

    public static OrderItem create(Long orderId, Long productVariantId, String productName, String variantName,
                                    String sku, BigDecimal unitPrice, int quantity) {
        return new OrderItem(orderId, productVariantId, productName, variantName, sku, unitPrice, quantity);
    }
}
