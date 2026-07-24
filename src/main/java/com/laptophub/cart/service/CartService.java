package com.laptophub.cart.service;

import com.laptophub.cart.entity.Cart;
import com.laptophub.cart.entity.CartItem;
import com.laptophub.cart.repository.CartItemRepository;
import com.laptophub.cart.repository.CartRepository;
import com.laptophub.catalog.service.ProductVariantService;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantService productVariantService;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
            ProductVariantService productVariantService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantService = productVariantService;
    }

    // Giỏ trống (chưa từng thêm gì) không tạo row Cart — chỉ trả list rỗng,
    // giống InventoryService.getBalance trả object mặc định mà không persist.
    public List<CartLine> getItemsWithLivePrice(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> cartItemRepository.findByCartId(cart.getId()).stream()
                        .map(item -> new CartLine(item, productVariantService.getByIdOrThrow(item.getProductVariantId())))
                        .toList())
                .orElseGet(List::of);
    }

    // Không ép variant phải ACTIVE ở bước thêm giỏ — checkout mới là nơi chặn
    // (PROJECT_RULES.md §6: checkout phải đọc lại trạng thái sản phẩm).
    @Transactional
    public CartItem addItem(Long userId, Long productVariantId, int quantity) {
        productVariantService.getByIdOrThrow(productVariantId);
        Cart cart = getOrCreateCart(userId);
        return cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), productVariantId)
                .map(existing -> {
                    existing.increaseQuantity(quantity);
                    return existing;
                })
                .orElseGet(() -> cartItemRepository.save(CartItem.create(cart.getId(), productVariantId, quantity)));
    }

    @Transactional
    public CartItem updateQuantity(Long userId, Long itemId, int quantity) {
        CartItem item = getOwnedItem(userId, itemId);
        item.changeQuantity(quantity);
        return item;
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {
        cartItemRepository.delete(getOwnedItem(userId, itemId));
    }

    @Transactional
    public void clear(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }

    // Không phân biệt "chưa có giỏ" và "item không thuộc giỏ của user này" —
    // cả 2 đều RESOURCE_NOT_FOUND, giống AddressService.getOwned.
    private CartItem getOwnedItem(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // Xử lý race lần tạo cart đầu tiên: 2 request cùng thêm sản phẩm đầu tiên
    // gần như đồng thời cho cùng 1 user — bên thua cuộc đọc lại row đã được
    // tạo thay vì lỗi, giống InventoryService.getOrCreateBalance.
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            try {
                return cartRepository.save(Cart.create(userId));
            } catch (DataIntegrityViolationException e) {
                return cartRepository.findByUserId(userId).orElseThrow(() -> e);
            }
        });
    }
}
