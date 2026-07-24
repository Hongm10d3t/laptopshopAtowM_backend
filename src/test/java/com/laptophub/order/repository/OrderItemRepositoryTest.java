package com.laptophub.order.repository;

import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.repository.BrandRepository;
import com.laptophub.catalog.repository.CategoryRepository;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.catalog.repository.ProductVariantRepository;
import com.laptophub.config.JpaAuditingConfig;
import com.laptophub.order.entity.Order;
import com.laptophub.order.entity.OrderItem;
import com.laptophub.user.EmailNormalizer;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class OrderItemRepositoryTest {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    private Long newOrderId(String slug) {
        User user = userRepository.saveAndFlush(
                User.create(EmailNormalizer.normalize(slug + "@example.com"), "hash", "Name", null,
                        UserRole.CUSTOMER));
        Order order = Order.create(user.getId(), BigDecimal.TEN, null, "A", "0900000000", "HN", "CG", "DV", "123");
        return orderRepository.saveAndFlush(order).getId();
    }

    private Long newVariantId(String slug) {
        Long categoryId = categoryRepository.saveAndFlush(Category.create("Cat " + slug, "cat-" + slug, null)).getId();
        Long brandId = brandRepository.saveAndFlush(Brand.create("Brand " + slug, "brand-" + slug, null, null)).getId();
        Long productId = productRepository
                .saveAndFlush(Product.create(categoryId, brandId, "Product " + slug, slug, null, null)).getId();
        return productVariantRepository
                .saveAndFlush(ProductVariant.create(productId, "SKU-" + slug, null, BigDecimal.TEN, null, null,
                        null, null))
                .getId();
    }

    @Test
    void findByOrderId_returnsAllItemsOfOrder() {
        Long orderId = newOrderId("order-items");
        Long variantId1 = newVariantId("oi-v1");
        Long variantId2 = newVariantId("oi-v2");
        orderItemRepository.saveAndFlush(
                OrderItem.create(orderId, variantId1, "Product 1", null, "SKU-1", BigDecimal.TEN, 1));
        orderItemRepository.saveAndFlush(
                OrderItem.create(orderId, variantId2, "Product 2", null, "SKU-2", BigDecimal.TEN, 2));

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        assertThat(items).hasSize(2);
    }

    @Test
    void findByOrderId_returnsEmpty_whenOrderHasNoItems() {
        Long orderId = newOrderId("order-no-items");

        assertThat(orderItemRepository.findByOrderId(orderId)).isEmpty();
    }
}
