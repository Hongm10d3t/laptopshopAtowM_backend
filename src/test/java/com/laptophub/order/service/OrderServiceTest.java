package com.laptophub.order.service;

import com.laptophub.cart.entity.CartItem;
import com.laptophub.cart.service.CartService;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.service.ProductService;
import com.laptophub.catalog.service.ProductVariantService;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.inventory.service.InventoryService;
import com.laptophub.order.dto.CheckoutRequest;
import com.laptophub.order.entity.Order;
import com.laptophub.order.entity.OrderItem;
import com.laptophub.order.entity.OrderStatus;
import com.laptophub.order.entity.OrderStatusHistory;
import com.laptophub.order.repository.OrderItemRepository;
import com.laptophub.order.repository.OrderRepository;
import com.laptophub.order.repository.OrderStatusHistoryRepository;
import com.laptophub.user.entity.Address;
import com.laptophub.user.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ADDRESS_ID = 2L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private CartService cartService;

    @Mock
    private AddressService addressService;

    @Mock
    private ProductVariantService productVariantService;

    @Mock
    private ProductService productService;

    @Mock
    private InventoryService inventoryService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, orderStatusHistoryRepository,
                cartService, addressService, productVariantService, productService, inventoryService);
        lenient().when(addressService.getOwned(USER_ID, ADDRESS_ID)).thenReturn(
                Address.create(USER_ID, "Nguyen Van A", "0900000000", "HN", "CG", "DV", "123 Duong ABC", true));
        lenient().when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        lenient().when(orderItemRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<OrderItem> items = invocation.getArgument(0);
            AtomicLong nextId = new AtomicLong(1000L);
            items.forEach(item -> item.setId(nextId.getAndIncrement()));
            return items;
        });
    }

    private CheckoutRequest checkoutRequest() {
        return new CheckoutRequest(ADDRESS_ID, "Giao giờ hành chính");
    }

    private ProductVariant activeVariant(Long id, Long productId, BigDecimal price) {
        ProductVariant variant = ProductVariant.create(productId, "SKU-" + id, "16GB/512GB", price, 16, 512, "SSD",
                "Black");
        variant.setId(id);
        return variant;
    }

    private Product activeProduct(Long id, String name) {
        Product product = Product.create(1L, 1L, name, "slug-" + id, null, null);
        product.setId(id);
        return product;
    }

    @Test
    void checkout_throwsValidationError_whenCartEmpty() {
        when(cartService.lockItemsForCheckout(USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout(USER_ID, checkoutRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(addressService, never()).getOwned(any(), any());
    }

    @Test
    void checkout_propagatesResourceNotFound_whenAddressNotOwned() {
        when(cartService.lockItemsForCheckout(USER_ID)).thenReturn(List.of(CartItem.create(1L, 10L, 1)));
        when(addressService.getOwned(USER_ID, ADDRESS_ID)).thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        assertThatThrownBy(() -> orderService.checkout(USER_ID, checkoutRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    @Test
    void checkout_throwsProductVariantUnavailable_whenVariantInactive() {
        when(cartService.lockItemsForCheckout(USER_ID)).thenReturn(List.of(CartItem.create(1L, 10L, 1)));
        ProductVariant inactiveVariant = activeVariant(10L, 20L, BigDecimal.TEN);
        inactiveVariant.deactivate();
        when(productVariantService.getByIdOrThrow(10L)).thenReturn(inactiveVariant);
        when(productService.getByIdOrThrow(20L)).thenReturn(activeProduct(20L, "Laptop ABC"));

        assertThatThrownBy(() -> orderService.checkout(USER_ID, checkoutRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PRODUCT_VARIANT_UNAVAILABLE));
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    @Test
    void checkout_throwsProductVariantUnavailable_whenProductInactive() {
        when(cartService.lockItemsForCheckout(USER_ID)).thenReturn(List.of(CartItem.create(1L, 10L, 1)));
        when(productVariantService.getByIdOrThrow(10L)).thenReturn(activeVariant(10L, 20L, BigDecimal.TEN));
        Product inactiveProduct = activeProduct(20L, "Laptop ABC");
        inactiveProduct.deactivate();
        when(productService.getByIdOrThrow(20L)).thenReturn(inactiveProduct);

        assertThatThrownBy(() -> orderService.checkout(USER_ID, checkoutRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PRODUCT_VARIANT_UNAVAILABLE));
    }

    @Test
    void checkout_success_computesTotal_createsOrderAndItems_reservesInventory_andClearsCart() {
        when(cartService.lockItemsForCheckout(USER_ID)).thenReturn(
                List.of(CartItem.create(1L, 10L, 2), CartItem.create(1L, 11L, 3)));
        when(productVariantService.getByIdOrThrow(10L)).thenReturn(activeVariant(10L, 20L, new BigDecimal("100.00")));
        when(productVariantService.getByIdOrThrow(11L)).thenReturn(activeVariant(11L, 21L, new BigDecimal("50.00")));
        when(productService.getByIdOrThrow(20L)).thenReturn(activeProduct(20L, "Laptop A"));
        when(productService.getByIdOrThrow(21L)).thenReturn(activeProduct(21L, "Laptop B"));

        CheckoutResult result = orderService.checkout(USER_ID, checkoutRequest());

        // 2 * 100.00 + 3 * 50.00 = 350.00
        assertThat(result.order().getTotalAmount()).isEqualByComparingTo("350.00");
        assertThat(result.order().getUserId()).isEqualTo(USER_ID);
        assertThat(result.order().getRecipientName()).isEqualTo("Nguyen Van A");
        assertThat(result.items()).hasSize(2);

        verify(inventoryService).reserve(10L, 2, "ORDER_ITEM", result.items().get(0).getId());
        verify(inventoryService).reserve(11L, 3, "ORDER_ITEM", result.items().get(1).getId());
        verify(cartService).clear(USER_ID);
    }

    @Test
    void checkout_insufficientStock_propagates_andDoesNotClearCart() {
        when(cartService.lockItemsForCheckout(USER_ID)).thenReturn(List.of(CartItem.create(1L, 10L, 5)));
        when(productVariantService.getByIdOrThrow(10L)).thenReturn(activeVariant(10L, 20L, BigDecimal.TEN));
        when(productService.getByIdOrThrow(20L)).thenReturn(activeProduct(20L, "Laptop A"));
        when(inventoryService.reserve(any(), anyInt(), any(), any()))
                .thenThrow(new AppException(ErrorCode.INSUFFICIENT_STOCK));

        assertThatThrownBy(() -> orderService.checkout(USER_ID, checkoutRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK));
        verify(cartService, never()).clear(any());
    }

    private Order orderWithStatus(Long id, OrderStatus status) {
        Order order = Order.create(USER_ID, BigDecimal.TEN, null, "A", "0900000000", "HN", "CG", "DV", "123");
        order.setId(id);
        if (status == OrderStatus.CONFIRMED || status == OrderStatus.PREPARING || status == OrderStatus.SHIPPING
                || status == OrderStatus.DELIVERED) {
            order.confirm();
        }
        if (status == OrderStatus.PREPARING || status == OrderStatus.SHIPPING || status == OrderStatus.DELIVERED) {
            order.prepare();
        }
        if (status == OrderStatus.SHIPPING || status == OrderStatus.DELIVERED) {
            order.ship();
        }
        if (status == OrderStatus.DELIVERED) {
            order.deliver();
        }
        return order;
    }

    @Test
    void getByIdOrThrow_throwsResourceNotFound_whenMissing() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getByIdOrThrow(999L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void listAdmin_delegatesToRepositorySearch() {
        orderService.listAdmin(OrderStatus.PENDING, null);

        verify(orderRepository).search(OrderStatus.PENDING, null);
    }

    @Test
    void confirm_transitionsOrder_andRecordsHistory() {
        Order order = orderWithStatus(100L, OrderStatus.PENDING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        Order result = orderService.confirm(100L, 5L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        ArgumentCaptor<OrderStatusHistory> captor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(orderStatusHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(captor.getValue().getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(captor.getValue().getChangedByUserId()).isEqualTo(5L);
    }

    @Test
    void confirm_propagatesInvalidOrderStatus_whenNotPending() {
        Order order = orderWithStatus(100L, OrderStatus.CONFIRMED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirm(100L, 5L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    void prepare_transitionsOrder_andRecordsHistory() {
        Order order = orderWithStatus(100L, OrderStatus.CONFIRMED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        Order result = orderService.prepare(100L, 5L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PREPARING);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void prepare_propagatesInvalidOrderStatus_whenNotConfirmed() {
        Order order = orderWithStatus(100L, OrderStatus.PENDING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.prepare(100L, 5L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
    }

    @Test
    void deliver_transitionsOrder_andRecordsHistory() {
        Order order = orderWithStatus(100L, OrderStatus.SHIPPING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        Order result = orderService.deliver(100L, 5L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void deliver_propagatesInvalidOrderStatus_whenNotShipping() {
        Order order = orderWithStatus(100L, OrderStatus.PREPARING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.deliver(100L, 5L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
    }

    @Test
    void ship_callsFulfillForEachItem_transitionsToShipping_andRecordsHistory() {
        Order order = orderWithStatus(100L, OrderStatus.PREPARING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        OrderItem item1 = OrderItem.create(100L, 10L, "Laptop A", null, "SKU-10", BigDecimal.TEN, 2);
        item1.setId(1000L);
        OrderItem item2 = OrderItem.create(100L, 11L, "Laptop B", null, "SKU-11", BigDecimal.TEN, 1);
        item2.setId(1001L);
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(item1, item2));

        Order result = orderService.ship(100L, 5L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPING);
        verify(inventoryService).fulfill(10L, 2, "ORDER_ITEM", 1000L);
        verify(inventoryService).fulfill(11L, 1, "ORDER_ITEM", 1001L);
        ArgumentCaptor<OrderStatusHistory> captor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(orderStatusHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(captor.getValue().getToStatus()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    void ship_rejectsWhenNotPreparing() {
        Order order = orderWithStatus(100L, OrderStatus.CONFIRMED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.ship(100L, 5L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
        verify(inventoryService, never()).fulfill(any(), anyInt(), any(), any());
        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    void ship_insufficientStock_propagates_andDoesNotRecordHistory() {
        Order order = orderWithStatus(100L, OrderStatus.PREPARING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        OrderItem item = OrderItem.create(100L, 10L, "Laptop A", null, "SKU-10", BigDecimal.TEN, 5);
        item.setId(1000L);
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(item));
        when(inventoryService.fulfill(any(), anyInt(), any(), any()))
                .thenThrow(new AppException(ErrorCode.INSUFFICIENT_STOCK));

        assertThatThrownBy(() -> orderService.ship(100L, 5L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK));
        verify(orderStatusHistoryRepository, never()).save(any());
    }
}
