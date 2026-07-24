package com.laptophub.order.service;

import com.laptophub.cart.service.CartService;
import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.repository.BrandRepository;
import com.laptophub.catalog.repository.CategoryRepository;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.catalog.repository.ProductVariantRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.inventory.service.InventoryService;
import com.laptophub.order.dto.CheckoutRequest;
import com.laptophub.order.repository.OrderRepository;
import com.laptophub.user.dto.AddressCreateRequest;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.repository.UserRepository;
import com.laptophub.user.service.AddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Không dùng @Transactional ở test này — cần transaction THẬT được commit
// riêng ở mỗi thread để chứng minh khoá pessimistic
// (CartRepository.findByUserIdForUpdate) serialize 2 request checkout đồng
// thời của cùng 1 user, đúng thiết kế ở "Quyết định thiết kế quan trọng"
// trong plan Giai đoạn 5. Nếu bọc @Transactional ở mức test, cả 2 lệnh gọi
// sẽ dùng chung 1 transaction của test và không phản ánh đúng tình huống race
// thực tế.
@SpringBootTest
class OrderServiceCheckoutConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Test
    void checkout_concurrentRequestsForSameUser_onlyOneSucceeds() throws Exception {
        String slug = "concurrent-" + UUID.randomUUID();

        User user = userRepository.saveAndFlush(
                User.create(slug + "@example.com", "hash", "Name", null, UserRole.CUSTOMER));
        Long userId = user.getId();

        Long categoryId = categoryRepository.saveAndFlush(Category.create("Cat " + slug, "cat-" + slug, null))
                .getId();
        Long brandId = brandRepository.saveAndFlush(Brand.create("Brand " + slug, "brand-" + slug, null, null))
                .getId();
        Long productId = productRepository
                .saveAndFlush(Product.create(categoryId, brandId, "Product " + slug, slug, null, null)).getId();
        Long variantId = productVariantRepository
                .saveAndFlush(ProductVariant.create(productId, "SKU-" + slug, null, new BigDecimal("100.00"), null,
                        null, null, null))
                .getId();
        inventoryService.receiveStock(variantId, 100, "TEST_SEED", null, null);

        var address = addressService.create(userId,
                new AddressCreateRequest("Nguyen Van A", "0900000000", "HN", "CG", "DV", "123 Duong ABC", true));
        Long addressId = address.getId();

        cartService.addItem(userId, variantId, 1);

        CountDownLatch startLatch = new CountDownLatch(1);
        Callable<CheckoutResult> checkoutTask = () -> {
            startLatch.await();
            return orderService.checkout(userId, new CheckoutRequest(addressId, null));
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<CheckoutResult> future1 = executor.submit(checkoutTask);
            Future<CheckoutResult> future2 = executor.submit(checkoutTask);
            startLatch.countDown();

            int successCount = 0;
            int emptyCartFailureCount = 0;
            for (Future<CheckoutResult> future : List.of(future1, future2)) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                    successCount++;
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    assertThat(cause).isInstanceOf(AppException.class);
                    assertThat(((AppException) cause).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    emptyCartFailureCount++;
                }
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(emptyCartFailureCount).isEqualTo(1);
        } finally {
            executor.shutdown();
        }

        assertThat(orderRepository.findByUserId(userId, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(inventoryService.getBalance(variantId).getReservedQuantity()).isEqualTo(1);
    }
}
