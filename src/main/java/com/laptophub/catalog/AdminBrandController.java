package com.laptophub.catalog;

import com.laptophub.catalog.dto.BrandCreateRequest;
import com.laptophub.catalog.dto.BrandResponse;
import com.laptophub.catalog.dto.BrandUpdateRequest;
import com.laptophub.catalog.service.BrandService;
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
import org.springframework.web.bind.annotation.RestController;

// Quyền truy cập (hasRole('ADMIN') cho /admin/**) đã khai ở SecurityConfig.
@RestController
@RequestMapping("/admin/brands")
public class AdminBrandController {

    private final BrandService brandService;

    public AdminBrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponse>> create(@Valid @RequestBody BrandCreateRequest request) {
        var brand = brandService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(BrandResponse.from(brand)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> update(@PathVariable Long id,
                                                              @Valid @RequestBody BrandUpdateRequest request) {
        var brand = brandService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(BrandResponse.from(brand)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BrandResponse>>> list(Pageable pageable) {
        var page = brandService.list(pageable).map(BrandResponse::from);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> getOne(@PathVariable Long id) {
        var brand = brandService.getByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(BrandResponse.from(brand)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<BrandResponse>> activate(@PathVariable Long id) {
        var brand = brandService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(BrandResponse.from(brand)));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<BrandResponse>> deactivate(@PathVariable Long id) {
        var brand = brandService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(BrandResponse.from(brand)));
    }
}
