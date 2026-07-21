package com.laptophub.catalog.dto;

import com.laptophub.catalog.entity.ProductSpecValue;
import com.laptophub.catalog.entity.SpecificationDefinition;

// code/label/unit/groupLabel không nằm trên ProductSpecValue (FK dạng Long
// phẳng) nên phải truyền SpecificationDefinition tương ứng vào từ ngoài —
// caller (ProductSpecValueService) batch fetch 1 lần cho cả danh sách, tránh
// N+1.
public record ProductSpecValueResponse(
        Long specificationDefinitionId,
        String code,
        String label,
        String unit,
        String groupLabel,
        String value) {

    public static ProductSpecValueResponse from(ProductSpecValue specValue, SpecificationDefinition definition) {
        return new ProductSpecValueResponse(
                definition.getId(),
                definition.getCode(),
                definition.getLabel(),
                definition.getUnit(),
                definition.getGroupLabel(),
                specValue.getValue());
    }
}
