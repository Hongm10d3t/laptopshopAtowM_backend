package com.laptophub.catalog;

import com.laptophub.catalog.dto.SpecificationDefinitionResponse;
import com.laptophub.catalog.service.SpecificationDefinitionService;
import com.laptophub.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Quyền truy cập (hasRole('ADMIN') cho /admin/**) đã khai ở SecurityConfig.
// Chỉ đọc — dữ liệu tham chiếu ít thay đổi, chưa có Admin CRUD ở giai đoạn
// này (xem SpecificationDefinition).
@RestController
@RequestMapping("/admin/specifications")
public class AdminSpecificationController {

    private final SpecificationDefinitionService specificationDefinitionService;

    public AdminSpecificationController(SpecificationDefinitionService specificationDefinitionService) {
        this.specificationDefinitionService = specificationDefinitionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SpecificationDefinitionResponse>>> list(
            @RequestParam(required = false) Long categoryId) {
        List<SpecificationDefinitionResponse> responses = specificationDefinitionService.listForCategory(categoryId)
                .stream()
                .map(SpecificationDefinitionResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
