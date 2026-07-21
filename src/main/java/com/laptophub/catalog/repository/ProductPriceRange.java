package com.laptophub.catalog.repository;

import java.math.BigDecimal;

// Projection cho ProductVariantRepository.findMinMaxPriceByProductIdIn —
// phục vụ hiển thị priceFrom/priceTo trong danh sách sản phẩm mà không phải
// query riêng cho từng dòng (N+1).
public interface ProductPriceRange {

    Long getProductId();

    BigDecimal getMinPrice();

    BigDecimal getMaxPrice();
}
