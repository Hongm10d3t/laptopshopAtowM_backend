package com.laptophub.order;

import com.laptophub.common.ApiResponse;
import com.laptophub.common.dto.PageResponse;
import com.laptophub.order.dto.OrderResponse;
import com.laptophub.order.dto.OrderSummaryResponse;
import com.laptophub.order.entity.Order;
import com.laptophub.order.entity.OrderStatus;
import com.laptophub.order.service.OrderService;
import com.laptophub.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Quyền truy cập (hasRole('ADMIN') cho /admin/**) đã khai ở SecurityConfig.
// page/size rời rồi tự dựng PageRequest — OrderRepository.search đã có
// ORDER BY cố định, giống lý do ở AdminStockReceiptController.
@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    public AdminOrderController(OrderService orderService, CurrentUserProvider currentUserProvider) {
        this.orderService = orderService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<OrderSummaryResponse> result = orderService.listAdmin(status, pageable).map(OrderSummaryResponse::from);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOne(@PathVariable Long id) {
        Order order = orderService.getByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(toResponse(order)));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirm(@PathVariable Long id) {
        Long actingAdminId = currentUserProvider.getCurrentUser().userId();
        Order order = orderService.confirm(id, actingAdminId);
        return ResponseEntity.ok(ApiResponse.success(toResponse(order)));
    }

    @PostMapping("/{id}/prepare")
    public ResponseEntity<ApiResponse<OrderResponse>> prepare(@PathVariable Long id) {
        Long actingAdminId = currentUserProvider.getCurrentUser().userId();
        Order order = orderService.prepare(id, actingAdminId);
        return ResponseEntity.ok(ApiResponse.success(toResponse(order)));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<ApiResponse<OrderResponse>> ship(@PathVariable Long id) {
        Long actingAdminId = currentUserProvider.getCurrentUser().userId();
        Order order = orderService.ship(id, actingAdminId);
        return ResponseEntity.ok(ApiResponse.success(toResponse(order)));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> deliver(@PathVariable Long id) {
        Long actingAdminId = currentUserProvider.getCurrentUser().userId();
        Order order = orderService.deliver(id, actingAdminId);
        return ResponseEntity.ok(ApiResponse.success(toResponse(order)));
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.from(order, orderService.getItems(order.getId()));
    }
}
