package com.laptophub.catalog.dto;

// Whitelist thay vì forward Sort thô từ client: "giá" không phải field trực
// tiếp của Product (nằm ở ProductVariant), forward Sort tuỳ ý có thể ném
// PropertyReferenceException. Resolve thủ công ở ProductSearchService.
public enum ProductSortOption {
    NEWEST,
    NAME_ASC,
    NAME_DESC,
    PRICE_ASC,
    PRICE_DESC
}
