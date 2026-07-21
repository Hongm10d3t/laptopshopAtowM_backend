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

@Entity
@Table(name = "brands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, length = 160, unique = true)
    private String slug;

    @Column(name = "description", length = 1000)
    private String description;

    // Chỉ lưu URL — không làm upload/storage ảnh ở giai đoạn này.
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BrandStatus status;

    private Brand(String name, String slug, String description, String logoUrl) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.description = description;
        this.logoUrl = logoUrl;
        this.status = BrandStatus.ACTIVE;
    }

    public static Brand create(String name, String slug, String description, String logoUrl) {
        return new Brand(name, slug, description, logoUrl);
    }

    public void update(String name, String slug, String description, String logoUrl) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.description = description;
        this.logoUrl = logoUrl;
    }

    public void activate() {
        this.status = BrandStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = BrandStatus.INACTIVE;
    }
}
