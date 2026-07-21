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

import java.util.Objects;

// categoryId/brandId là FK dạng Long phẳng (không @ManyToOne) — đúng tiền lệ
// Address.userId/RefreshToken.userId, tránh lazy-loading/N+1.
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, length = 280, unique = true)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    private Product(Long categoryId, Long brandId, String name, String slug, String shortDescription,
                     String description) {
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        this.brandId = Objects.requireNonNull(brandId, "brandId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.shortDescription = shortDescription;
        this.description = description;
        this.status = ProductStatus.ACTIVE;
    }

    public static Product create(Long categoryId, Long brandId, String name, String slug, String shortDescription,
                                  String description) {
        return new Product(categoryId, brandId, name, slug, shortDescription, description);
    }

    public void update(String name, String slug, String shortDescription, String description) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.shortDescription = shortDescription;
        this.description = description;
    }

    public void changeCategory(Long categoryId) {
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
    }

    public void changeBrand(Long brandId) {
        this.brandId = Objects.requireNonNull(brandId, "brandId must not be null");
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }
}
