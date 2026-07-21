package com.laptophub.catalog;

import com.laptophub.catalog.dto.ProductDetailResponse;
import com.laptophub.catalog.dto.ProductListItemResponse;
import com.laptophub.catalog.dto.ProductSortOption;
import com.laptophub.catalog.service.ProductSearchService;
import com.laptophub.common.ApiResponse;
import com.laptophub.common.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

// /public/** đã permitAll() ở SecurityConfig.
// Không dùng Pageable trực tiếp làm tham số: query param "sort" ở đây là
// whitelist ProductSortOption riêng (giá không phải field trực tiếp của
// Product), trùng tên với "sort" mà Spring tự hiểu cho Pageable — nhận
// page/size rời rồi tự dựng PageRequest để tránh xung đột 2 cơ chế.
@RestController
@RequestMapping("/public/products")
public class PublicProductController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ProductSearchService productSearchService;

    public PublicProductController(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductListItemResponse>>> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "NEWEST") ProductSortOption sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        var result = productSearchService.search(categoryId, brandId, normalizedKeyword, minPrice, maxPrice, sort,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getDetail(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productSearchService.getDetailBySlug(slug)));
    }
}
