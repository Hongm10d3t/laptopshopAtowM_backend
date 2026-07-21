package com.laptophub.catalog.entity;

import com.laptophub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

// Không có status: ảnh chỉ mang tính trình bày, không liên quan giao dịch
// (khác Category/Brand/Product/Variant), nên hard-delete là đủ, không cần
// soft-deactivate. Chỉ lưu URL — không có upload/storage ảnh ở giai đoạn này.
// Ảnh đại diện = ảnh có sortOrder nhỏ nhất, không thêm cờ is_primary riêng
// (tránh 2 nguồn sự thật).
@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    private ProductImage(Long productId, String url, String altText, int sortOrder) {
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        this.url = Objects.requireNonNull(url, "url must not be null");
        this.altText = altText;
        this.sortOrder = sortOrder;
    }

    public static ProductImage create(Long productId, String url, String altText, int sortOrder) {
        return new ProductImage(productId, url, altText, sortOrder);
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void changeAltText(String altText) {
        this.altText = altText;
    }
}
