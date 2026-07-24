package com.laptophub.catalog;

import com.laptophub.catalog.dto.CategoryCreateRequest;
import com.laptophub.catalog.dto.CategoryResponse;
import com.laptophub.catalog.dto.CategoryUpdateRequest;
import com.laptophub.catalog.service.CategoryService;
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
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryCreateRequest request) {
        var category = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(CategoryResponse.from(category)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        var category = categoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(CategoryResponse.from(category)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> list(Pageable pageable) {
        var page = categoryService.list(pageable).map(CategoryResponse::from);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getOne(@PathVariable Long id) {
        var category = categoryService.getByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(CategoryResponse.from(category)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<CategoryResponse>> activate(@PathVariable Long id) {
        var category = categoryService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(CategoryResponse.from(category)));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<CategoryResponse>> deactivate(@PathVariable Long id) {
        var category = categoryService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(CategoryResponse.from(category)));
    }
}
