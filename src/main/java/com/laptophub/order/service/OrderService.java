package com.laptophub.order.service;

import com.laptophub.cart.entity.CartItem;
import com.laptophub.cart.service.CartService;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductStatus;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.entity.ProductVariantStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CartService cartService;
    private final AddressService addressService;
    private final ProductVariantService productVariantService;
    private final ProductService productService;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository, CartService cartService,
            AddressService addressService, ProductVariantService productVariantService,
            ProductService productService, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.cartService = cartService;
        this.addressService = addressService;
        this.productVariantService = productVariantService;
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    // Tạo đơn + reserve tồn trong cùng 1 transaction (DATABASE_DESIGN.md §4).
    // lockItemsForCheckout khoá pessimistic trên Cart ngay từ đầu, serialize
    // các request checkout đồng thời của cùng user — tránh double-order khi
    // double-click/network retry (API_CONVENTION.md §8).
    @Transactional
    public CheckoutResult checkout(Long userId, CheckoutRequest request) {
        List<CartItem> cartItems = cartService.lockItemsForCheckout(userId);
        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Giỏ hàng trống");
        }

        Address address = addressService.getOwned(userId, request.addressId());

        List<CheckoutLine> lines = cartItems.stream().map(this::resolveLine).toList();

        BigDecimal totalAmount = lines.stream()
                .map(line -> line.variant().getPrice().multiply(BigDecimal.valueOf(line.cartItem().getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = orderRepository.saveAndFlush(Order.create(userId, totalAmount, request.note(),
                address.getRecipientName(), address.getPhone(), address.getProvince(), address.getDistrict(),
                address.getWard(), address.getStreetAddress()));

        List<OrderItem> items = orderItemRepository.saveAllAndFlush(lines.stream()
                .map(line -> OrderItem.create(order.getId(), line.variant().getId(), line.product().getName(),
                        line.variant().getVariantName(), line.variant().getSku(), line.variant().getPrice(),
                        line.cartItem().getQuantity()))
                .toList());

        // InventoryBalanceRepository dùng @Modifying(clearAutomatically = true) —
        // mỗi lần reserve() xoá persistence context, Order/OrderItem phía trên bị
        // detach. Không sao vì không cần mutate lại chúng sau bước này.
        for (OrderItem item : items) {
            inventoryService.reserve(item.getProductVariantId(), item.getQuantity(), "ORDER_ITEM", item.getId());
        }

        cartService.clear(userId);

        return new CheckoutResult(order, items);
    }

    public Page<Order> listByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    // Không phân biệt "không tồn tại" và "không phải của user này" — cả 2
    // đều RESOURCE_NOT_FOUND, giống AddressService.getOwned.
    public Order getOwnedOrThrow(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public List<OrderItem> getItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    // Dùng cho Admin xem/thao tác bất kỳ đơn nào, không giới hạn theo chủ đơn
    // (khác getOwnedOrThrow).
    public Order getByIdOrThrow(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public Page<Order> listAdmin(OrderStatus status, Pageable pageable) {
        return orderRepository.search(status, pageable);
    }

    // confirm/prepare/deliver không đụng inventory nên không có rủi ro
    // clearAutomatically — nhưng vẫn phải saveAndFlush tường minh sau khi
    // mutate: dựa vào auto-flush ngầm của Hibernate (chỉ chắc chắn xảy ra khi
    // transaction thật sự commit) không đáng tin cậy khi nhiều lệnh gọi
    // @Transactional nối tiếp cùng tham gia 1 transaction (vd nhiều request
    // liên tiếp trong 1 test @Transactional) — thay đổi có thể chỉ nằm trên
    // object Java và bị mất khi request kế tiếp đọc lại. saveAndFlush ép ghi
    // UPDATE ngay lập tức, không phụ thuộc thời điểm auto-flush.
    @Transactional
    public Order confirm(Long orderId, Long actingUserId) {
        Order order = getByIdOrThrow(orderId);
        OrderStatus previous = order.getStatus();
        order.confirm();
        orderRepository.saveAndFlush(order);
        recordHistory(orderId, previous, order.getStatus(), actingUserId, null);
        return order;
    }

    @Transactional
    public Order prepare(Long orderId, Long actingUserId) {
        Order order = getByIdOrThrow(orderId);
        OrderStatus previous = order.getStatus();
        order.prepare();
        orderRepository.saveAndFlush(order);
        recordHistory(orderId, previous, order.getStatus(), actingUserId, null);
        return order;
    }

    @Transactional
    public Order deliver(Long orderId, Long actingUserId) {
        Order order = getByIdOrThrow(orderId);
        OrderStatus previous = order.getStatus();
        order.deliver();
        orderRepository.saveAndFlush(order);
        recordHistory(orderId, previous, order.getStatus(), actingUserId, null);
        return order;
    }

    // Giảm on_hand khi xuất đơn (DATABASE_DESIGN.md §4). Thứ tự bắt buộc theo
    // đúng pattern StockReceiptService.confirm: đọc status trước (fail-fast,
    // chưa mutate) -> lặp gọi inventoryService.fulfill cho từng dòng (mỗi lần
    // gọi xoá persistence context vì InventoryBalanceRepository dùng
    // @Modifying(clearAutomatically = true), khiến `order` đã load bị detach)
    // -> LOAD LẠI Order quản lý mới rồi mới gọi .ship(). Nếu mutate trên
    // `order` cũ trước vòng lặp, thay đổi status chỉ nằm trên object Java và
    // không bao giờ được flush xuống DB — bug âm thầm, không có exception.
    @Transactional
    public Order ship(Long orderId, Long actingUserId) {
        Order order = getByIdOrThrow(orderId);
        if (order.getStatus() != OrderStatus.PREPARING) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        OrderStatus previous = order.getStatus();

        List<OrderItem> items = getItems(orderId);
        for (OrderItem item : items) {
            inventoryService.fulfill(item.getProductVariantId(), item.getQuantity(), "ORDER_ITEM", item.getId());
        }

        Order managedOrder = getByIdOrThrow(orderId);
        managedOrder.ship();
        orderRepository.saveAndFlush(managedOrder);
        recordHistory(orderId, previous, managedOrder.getStatus(), actingUserId, null);
        return managedOrder;
    }

    private void recordHistory(Long orderId, OrderStatus from, OrderStatus to, Long changedByUserId, String note) {
        orderStatusHistoryRepository.save(OrderStatusHistory.create(orderId, from, to, changedByUserId, note));
    }

    private CheckoutLine resolveLine(CartItem cartItem) {
        ProductVariant variant = productVariantService.getByIdOrThrow(cartItem.getProductVariantId());
        Product product = productService.getByIdOrThrow(variant.getProductId());
        if (variant.getStatus() != ProductVariantStatus.ACTIVE || product.getStatus() != ProductStatus.ACTIVE) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_UNAVAILABLE,
                    "Sản phẩm \"" + product.getName() + "\" hiện không khả dụng để đặt hàng");
        }
        return new CheckoutLine(cartItem, variant, product);
    }

    private record CheckoutLine(CartItem cartItem, ProductVariant variant, Product product) {
    }
}
