package com.laptophub.catalog.entity;

import com.laptophub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

// productId là FK dạng Long phẳng (không @ManyToOne), đúng tiền lệ chung của
// module catalog. ramGb/storageGb/storageType/color là cột tường minh (không
// EAV) — đây là tập thuộc tính hữu hạn quyết định SKU khác nhau, khác với
// thông số kỹ thuật tự do của Product (SpecificationDefinition/ProductSpecValue).
@Entity
@Table(name = "product_variants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku", nullable = false, length = 100, unique = true)
    private String sku;

    @Column(name = "variant_name", length = 255)
    private String variantName;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "ram_gb")
    private Integer ramGb;

    @Column(name = "storage_gb")
    private Integer storageGb;

    @Column(name = "storage_type", length = 20)
    private String storageType;

    @Column(name = "color", length = 50)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductVariantStatus status;

    private ProductVariant(Long productId, String sku, String variantName, BigDecimal price, Integer ramGb,
                            Integer storageGb, String storageType, String color) {
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        this.sku = Objects.requireNonNull(sku, "sku must not be null");
        this.variantName = variantName;
        this.price = Objects.requireNonNull(price, "price must not be null");
        this.ramGb = ramGb;
        this.storageGb = storageGb;
        this.storageType = storageType;
        this.color = color;
        this.status = ProductVariantStatus.ACTIVE;
    }

    public static ProductVariant create(Long productId, String sku, String variantName, BigDecimal price,
                                         Integer ramGb, Integer storageGb, String storageType, String color) {
        return new ProductVariant(productId, sku, variantName, price, ramGb, storageGb, storageType, color);
    }

    public void update(String variantName, BigDecimal price, Integer ramGb, Integer storageGb, String storageType,
                        String color) {
        this.variantName = variantName;
        this.price = Objects.requireNonNull(price, "price must not be null");
        this.ramGb = ramGb;
        this.storageGb = storageGb;
        this.storageType = storageType;
        this.color = color;
    }

    public void activate() {
        this.status = ProductVariantStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ProductVariantStatus.INACTIVE;
    }
}
