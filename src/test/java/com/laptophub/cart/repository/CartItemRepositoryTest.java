package com.laptophub.cart.repository;

import com.laptophub.cart.entity.Cart;
import com.laptophub.cart.entity.CartItem;
import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.repository.BrandRepository;
import com.laptophub.catalog.repository.CategoryRepository;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.catalog.repository.ProductVariantRepository;
import com.laptophub.config.JpaAuditingConfig;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class CartItemRepositoryTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

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

    private Long newCartId(String slug) {
        User user = userRepository.saveAndFlush(
                User.create(EmailNormalizer.normalize(slug + "@example.com"), "hash", "Name", null,
                        UserRole.CUSTOMER));
        return cartRepository.saveAndFlush(Cart.create(user.getId())).getId();
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
    void findByCartId_returnsAllItemsOfCart() {
        Long cartId = newCartId("cart-list");
        Long variantId1 = newVariantId("v1");
        Long variantId2 = newVariantId("v2");
        cartItemRepository.saveAndFlush(CartItem.create(cartId, variantId1, 1));
        cartItemRepository.saveAndFlush(CartItem.create(cartId, variantId2, 2));

        List<CartItem> items = cartItemRepository.findByCartId(cartId);

        assertThat(items).hasSize(2);
    }

    @Test
    void findByCartIdAndProductVariantId_returnsEmpty_whenNotInCart() {
        Long cartId = newCartId("cart-find-empty");
        Long variantId = newVariantId("find-empty");

        assertThat(cartItemRepository.findByCartIdAndProductVariantId(cartId, variantId)).isEmpty();
    }

    @Test
    void findByCartIdAndProductVariantId_returnsItem_whenPresent() {
        Long cartId = newCartId("cart-find");
        Long variantId = newVariantId("find");
        CartItem saved = cartItemRepository.saveAndFlush(CartItem.create(cartId, variantId, 1));

        Optional<CartItem> found = cartItemRepository.findByCartIdAndProductVariantId(cartId, variantId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByIdAndCartId_returnsEmpty_whenItemBelongsToAnotherCart() {
        Long ownerCartId = newCartId("owner-cart");
        Long strangerCartId = newCartId("stranger-cart");
        Long variantId = newVariantId("ownership");
        CartItem item = cartItemRepository.saveAndFlush(CartItem.create(ownerCartId, variantId, 1));

        assertThat(cartItemRepository.findByIdAndCartId(item.getId(), ownerCartId)).isPresent();
        assertThat(cartItemRepository.findByIdAndCartId(item.getId(), strangerCartId)).isEmpty();
    }

    @Test
    void deleteByCartId_removesAllItemsOfCart() {
        Long cartId = newCartId("cart-clear");
        Long variantId1 = newVariantId("clear1");
        Long variantId2 = newVariantId("clear2");
        cartItemRepository.saveAndFlush(CartItem.create(cartId, variantId1, 1));
        cartItemRepository.saveAndFlush(CartItem.create(cartId, variantId2, 2));

        cartItemRepository.deleteByCartId(cartId);

        assertThat(cartItemRepository.findByCartId(cartId)).isEmpty();
    }
}
