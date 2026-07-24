package com.laptophub.cart.service;

import com.laptophub.cart.entity.Cart;
import com.laptophub.cart.entity.CartItem;
import com.laptophub.cart.repository.CartItemRepository;
import com.laptophub.cart.repository.CartRepository;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.service.ProductVariantService;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long VARIANT_ID = 2L;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductVariantService productVariantService;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository, productVariantService);
        lenient().when(productVariantService.getByIdOrThrow(VARIANT_ID))
                .thenReturn(ProductVariant.create(1L, "SKU-001", null, BigDecimal.TEN, null, null, null, null));
    }

    private Cart cartWithId(Long id) {
        Cart cart = Cart.create(USER_ID);
        cart.setId(id);
        return cart;
    }

    @Test
    void getItemsWithLivePrice_returnsEmptyList_whenNoCartYet() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(cartService.getItemsWithLivePrice(USER_ID)).isEmpty();
    }

    @Test
    void getItemsWithLivePrice_returnsLines_withLiveVariant() {
        Cart cart = cartWithId(10L);
        CartItem item = CartItem.create(10L, VARIANT_ID, 2);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(item));

        List<CartLine> lines = cartService.getItemsWithLivePrice(USER_ID);

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).item()).isEqualTo(item);
        assertThat(lines.get(0).variant().getSku()).isEqualTo("SKU-001");
    }

    @Test
    void addItem_throwsResourceNotFound_whenVariantMissing() {
        when(productVariantService.getByIdOrThrow(99L)).thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, 99L, 1))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void addItem_createsCartAndItem_whenNothingExistsYet() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        Cart savedCart = cartWithId(10L);
        when(cartRepository.save(any(Cart.class))).thenReturn(savedCart);
        when(cartItemRepository.findByCartIdAndProductVariantId(10L, VARIANT_ID)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartLine created = cartService.addItem(USER_ID, VARIANT_ID, 3);

        assertThat(created.item().getCartId()).isEqualTo(10L);
        assertThat(created.item().getProductVariantId()).isEqualTo(VARIANT_ID);
        assertThat(created.item().getQuantity()).isEqualTo(3);
        assertThat(created.variant().getSku()).isEqualTo("SKU-001");
    }

    @Test
    void addItem_increasesQuantity_whenVariantAlreadyInCart() {
        Cart cart = cartWithId(10L);
        CartItem existing = CartItem.create(10L, VARIANT_ID, 2);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductVariantId(10L, VARIANT_ID)).thenReturn(Optional.of(existing));

        CartLine result = cartService.addItem(USER_ID, VARIANT_ID, 3);

        assertThat(result.item().getQuantity()).isEqualTo(5);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateQuantity_throwsResourceNotFound_whenCartMissing() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(USER_ID, 5L, 1))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void updateQuantity_throwsResourceNotFound_whenItemNotInCart() {
        Cart cart = cartWithId(10L);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(5L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(USER_ID, 5L, 1))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void updateQuantity_replacesQuantity_onOwnedItem() {
        Cart cart = cartWithId(10L);
        CartItem item = CartItem.create(10L, VARIANT_ID, 2);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(5L, 10L)).thenReturn(Optional.of(item));

        CartLine result = cartService.updateQuantity(USER_ID, 5L, 9);

        assertThat(result.item().getQuantity()).isEqualTo(9);
        assertThat(result.variant().getSku()).isEqualTo("SKU-001");
    }

    @Test
    void removeItem_deletesOwnedItem() {
        Cart cart = cartWithId(10L);
        CartItem item = CartItem.create(10L, VARIANT_ID, 2);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(5L, 10L)).thenReturn(Optional.of(item));

        cartService.removeItem(USER_ID, 5L);

        verify(cartItemRepository).delete(item);
    }

    @Test
    void clear_deletesAllItems_whenCartExists() {
        Cart cart = cartWithId(10L);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        cartService.clear(USER_ID);

        verify(cartItemRepository).deleteByCartId(10L);
    }

    @Test
    void clear_doesNothing_whenNoCartYet() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        cartService.clear(USER_ID);

        verify(cartItemRepository, never()).deleteByCartId(any());
    }
}
