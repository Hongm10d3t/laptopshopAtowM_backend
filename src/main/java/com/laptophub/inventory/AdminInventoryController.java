package com.laptophub.inventory;

import com.laptophub.catalog.service.ProductVariantService;
import com.laptophub.common.ApiResponse;
import com.laptophub.common.dto.PageResponse;
import com.laptophub.inventory.dto.InventoryAdjustRequest;
import com.laptophub.inventory.dto.InventoryBalanceResponse;
import com.laptophub.inventory.dto.InventoryMovementResponse;
import com.laptophub.inventory.entity.InventoryBalance;
import com.laptophub.inventory.entity.InventoryMovementType;
import com.laptophub.inventory.service.InventoryService;
import com.laptophub.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Quyền truy cập (hasRole('ADMIN') cho /admin/**) đã khai ở SecurityConfig.
// page/size rời rồi tự dựng PageRequest (không bind Pageable thẳng) — JPQL
// của InventoryMovementRepository đã có ORDER BY cố định (mới nhất trước),
// bind Pageable trực tiếp sẽ cho phép client truyền ?sort= gây double ORDER
// BY, giống lý do PublicProductController tự dựng PageRequest.
@RestController
@RequestMapping("/admin/inventory")
public class AdminInventoryController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final InventoryService inventoryService;
    private final ProductVariantService productVariantService;
    private final CurrentUserProvider currentUserProvider;

    public AdminInventoryController(InventoryService inventoryService, ProductVariantService productVariantService,
            CurrentUserProvider currentUserProvider) {
        this.inventoryService = inventoryService;
        this.productVariantService = productVariantService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/{variantId}/balance")
    public ResponseEntity<ApiResponse<InventoryBalanceResponse>> getBalance(@PathVariable Long variantId) {
        InventoryBalance balance = inventoryService.getBalance(variantId);
        return ResponseEntity.ok(ApiResponse.success(toResponse(balance)));
    }

    @GetMapping("/{variantId}/movements")
    public ResponseEntity<ApiResponse<PageResponse<InventoryMovementResponse>>> listMovements(
            @PathVariable Long variantId, @RequestParam(required = false) InventoryMovementType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<InventoryMovementResponse> result = inventoryService.listMovements(variantId, type, pageable)
                .map(InventoryMovementResponse::from);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @PostMapping("/{variantId}/adjustments")
    public ResponseEntity<ApiResponse<InventoryBalanceResponse>> adjust(@PathVariable Long variantId,
            @Valid @RequestBody InventoryAdjustRequest request) {
        Long actingAdminId = currentUserProvider.getCurrentUser().userId();
        InventoryBalance balance = inventoryService.adjust(variantId, request.delta(), request.reason(),
                actingAdminId);
        return ResponseEntity.ok(ApiResponse.success(toResponse(balance)));
    }

    private InventoryBalanceResponse toResponse(InventoryBalance balance) {
        String sku = productVariantService.getByIdOrThrow(balance.getProductVariantId()).getSku();
        return InventoryBalanceResponse.from(balance, sku);
    }
}
