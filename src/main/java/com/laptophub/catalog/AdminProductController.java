package com.laptophub.catalog;

import com.laptophub.catalog.dto.ProductCreateRequest;
import com.laptophub.catalog.dto.ProductResponse;
import com.laptophub.catalog.dto.ProductSummaryResponse;
import com.laptophub.catalog.dto.ProductUpdateRequest;
import com.laptophub.catalog.dto.ProductVariantCreateRequest;
import com.laptophub.catalog.dto.ProductVariantResponse;
import com.laptophub.catalog.dto.ProductVariantUpdateRequest;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductStatus;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.service.BrandService;
import com.laptophub.catalog.service.CategoryService;
import com.laptophub.catalog.service.ProductService;
import com.laptophub.catalog.service.ProductVariantService;
import com.laptophub.common.ApiResponse;
import com.laptophub.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Quyền truy cập (hasRole('ADMIN') cho /admin/**) đã khai ở SecurityConfig.
// CRUD lõi Product + endpoint variant ở gói này — endpoint ảnh/thông số kỹ
// thuật sẽ mở rộng thêm ở các gói sau, cùng controller này.
@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final ProductVariantService productVariantService;

    public AdminProductController(ProductService productService, CategoryService categoryService,
                                   BrandService brandService, ProductVariantService productVariantService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.productVariantService = productVariantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductCreateRequest request) {
        Product product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toResponse(product)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody ProductUpdateRequest request) {
        Product product = productService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(toResponse(product)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
        var page = productService.listAdmin(categoryId, brandId, status, normalizedKeyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getOne(@PathVariable Long id) {
        Product product = productService.getByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(toResponse(product)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ProductResponse>> activate(@PathVariable Long id) {
        Product product = productService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(toResponse(product)));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ProductResponse>> deactivate(@PathVariable Long id) {
        Product product = productService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(toResponse(product)));
    }

    @PostMapping("/{id}/variants")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> addVariant(
            @PathVariable Long id, @Valid @RequestBody ProductVariantCreateRequest request) {
        ProductVariant variant = productVariantService.addVariant(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ProductVariantResponse.from(variant)));
    }

    @PutMapping("/{id}/variants/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long id, @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantUpdateRequest request) {
        ProductVariant variant = productVariantService.updateVariant(id, variantId, request);
        return ResponseEntity.ok(ApiResponse.success(ProductVariantResponse.from(variant)));
    }

    @PostMapping("/{id}/variants/{variantId}/activate")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> activateVariant(
            @PathVariable Long id, @PathVariable Long variantId) {
        ProductVariant variant = productVariantService.activate(id, variantId);
        return ResponseEntity.ok(ApiResponse.success(ProductVariantResponse.from(variant)));
    }

    @PostMapping("/{id}/variants/{variantId}/deactivate")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> deactivateVariant(
            @PathVariable Long id, @PathVariable Long variantId) {
        ProductVariant variant = productVariantService.deactivate(id, variantId);
        return ResponseEntity.ok(ApiResponse.success(ProductVariantResponse.from(variant)));
    }

    // 1 sản phẩm/lần nên tra tên category/brand rời qua getByIdOrThrow là đủ
    // — không cần batch fetch như ProductService.listAdmin (nhiều dòng).
    private ProductResponse toResponse(Product product) {
        String categoryName = categoryService.getByIdOrThrow(product.getCategoryId()).getName();
        String brandName = brandService.getByIdOrThrow(product.getBrandId()).getName();
        return ProductResponse.from(product, categoryName, brandName);
    }
}
