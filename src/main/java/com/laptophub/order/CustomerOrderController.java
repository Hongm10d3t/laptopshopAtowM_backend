package com.laptophub.order;

import com.laptophub.common.ApiResponse;
import com.laptophub.common.dto.PageResponse;
import com.laptophub.order.dto.CheckoutRequest;
import com.laptophub.order.dto.OrderResponse;
import com.laptophub.order.dto.OrderSummaryResponse;
import com.laptophub.order.entity.Order;
import com.laptophub.order.service.CheckoutResult;
import com.laptophub.order.service.OrderService;
import com.laptophub.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer/orders")
public class CustomerOrderController {

    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    public CustomerOrderController(OrderService orderService, CurrentUserProvider currentUserProvider) {
        this.orderService = orderService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(@Valid @RequestBody CheckoutRequest request) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        CheckoutResult result = orderService.checkout(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(OrderResponse.from(result.order(), result.items())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> list(Pageable pageable) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        var page = orderService.listByUser(userId, pageable).map(OrderSummaryResponse::from);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOne(@PathVariable Long id) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        Order order = orderService.getOwnedOrThrow(userId, id);
        var items = orderService.getItems(order.getId());
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.from(order, items)));
    }
}
