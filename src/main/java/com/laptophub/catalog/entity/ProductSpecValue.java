package com.laptophub.catalog.entity;

import com.laptophub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

// productId/specificationDefinitionId là FK dạng Long phẳng, đúng tiền lệ
// chung của module catalog.
@Entity
@Table(name = "product_spec_values")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSpecValue extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "specification_id", nullable = false)
    private Long specificationDefinitionId;

    @Column(name = "value", nullable = false, length = 500)
    private String value;

    private ProductSpecValue(Long productId, Long specificationDefinitionId, String value) {
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        this.specificationDefinitionId =
                Objects.requireNonNull(specificationDefinitionId, "specificationDefinitionId must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public static ProductSpecValue create(Long productId, Long specificationDefinitionId, String value) {
        return new ProductSpecValue(productId, specificationDefinitionId, value);
    }

    public void changeValue(String value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }
}
