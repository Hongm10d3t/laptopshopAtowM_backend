package com.laptophub.catalog;

import com.laptophub.catalog.dto.CategoryPublicResponse;
import com.laptophub.catalog.service.CategoryService;
import com.laptophub.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// /public/** đã permitAll() ở SecurityConfig.
@RestController
@RequestMapping("/public/categories")
public class PublicCategoryController {

    private final CategoryService categoryService;

    public PublicCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryPublicResponse>>> list() {
        List<CategoryPublicResponse> responses = categoryService.listActive().stream()
                .map(CategoryPublicResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
