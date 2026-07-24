package com.laptophub.cart;

import com.laptophub.cart.dto.CartItemAddRequest;
import com.laptophub.cart.dto.CartItemResponse;
import com.laptophub.cart.dto.CartItemUpdateRequest;
import com.laptophub.cart.dto.CartResponse;
import com.laptophub.cart.service.CartService;
import com.laptophub.common.ApiResponse;
import com.laptophub.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer/cart")
public class CustomerCartController {

    private final CartService cartService;
    private final CurrentUserProvider currentUserProvider;

    public CustomerCartController(CartService cartService, CurrentUserProvider currentUserProvider) {
        this.cartService = cartService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> view() {
        Long userId = currentUserProvider.getCurrentUser().userId();
        return ResponseEntity.ok(ApiResponse.success(CartResponse.from(cartService.getItemsWithLivePrice(userId))));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItemResponse>> addItem(@Valid @RequestBody CartItemAddRequest request) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        var line = cartService.addItem(userId, request.productVariantId(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(CartItemResponse.from(line)));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateQuantity(@PathVariable Long itemId,
            @Valid @RequestBody CartItemUpdateRequest request) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        var line = cartService.updateQuantity(userId, itemId, request.quantity());
        return ResponseEntity.ok(ApiResponse.success(CartItemResponse.from(line)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long itemId) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        cartService.removeItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear() {
        Long userId = currentUserProvider.getCurrentUser().userId();
        cartService.clear(userId);
        return ResponseEntity.noContent().build();
    }
}
