package com.laptophub.catalog.entity;

import com.laptophub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

// Đặt tên "SpecificationDefinition" (không phải "Specification") để tránh
// nhầm với org.springframework.data.jpa.domain.Specification<T> — dễ gây
// import shadowing nếu dùng chung tên trong codebase có Spring Data JPA.
//
// categoryId = null nghĩa là spec áp dụng toàn cục (mọi danh mục); có giá trị
// nghĩa là chỉ áp dụng cho 1 danh mục cụ thể. Đây là dữ liệu tham chiếu ít
// thay đổi — seed sẵn qua migration (V12), không có Admin CRUD ở giai đoạn
// này (chỉ đọc), nên không cần method update/xoá.
@Entity
@Table(name = "specifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpecificationDefinition extends BaseEntity {

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "code", nullable = false, length = 100, unique = true)
    private String code;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "group_label", length = 100)
    private String groupLabel;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private SpecificationDefinition(Long categoryId, String code, String label, String unit, String groupLabel,
                                     int displayOrder) {
        this.categoryId = categoryId;
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.label = Objects.requireNonNull(label, "label must not be null");
        this.unit = unit;
        this.groupLabel = groupLabel;
        this.displayOrder = displayOrder;
    }

    public static SpecificationDefinition create(Long categoryId, String code, String label, String unit,
                                                  String groupLabel, int displayOrder) {
        return new SpecificationDefinition(categoryId, code, label, unit, groupLabel, displayOrder);
    }
}
