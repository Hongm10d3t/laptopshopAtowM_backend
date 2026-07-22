package com.laptophub.inventory.repository;

import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.repository.BrandRepository;
import com.laptophub.catalog.repository.CategoryRepository;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.catalog.repository.ProductVariantRepository;
import com.laptophub.config.JpaAuditingConfig;
import com.laptophub.inventory.entity.InventoryBalance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// Chứng minh hành vi "update có điều kiện" bằng cách gọi tuần tự trong cùng 1
// test — không cần thread thật vì mỗi UPDATE ... WHERE là 1 statement atomic
// ở tầng InnoDB (row lock tự serialize writer đồng thời).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class InventoryBalanceRepositoryTest {

    @Autowired
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

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

    private Long seedBalance(String slug, int onHand, int reserved) {
        Long variantId = newVariantId(slug);
        inventoryBalanceRepository.saveAndFlush(InventoryBalance.create(variantId));
        if (onHand > 0) {
            inventoryBalanceRepository.applyOnHandDelta(variantId, onHand);
        }
        if (reserved > 0) {
            inventoryBalanceRepository.reserveQuantity(variantId, reserved);
        }
        return variantId;
    }

    @Test
    void findByProductVariantId_returnsEmpty_whenNoBalanceYet() {
        Long variantId = newVariantId("no-balance");

        assertThat(inventoryBalanceRepository.findByProductVariantId(variantId)).isEmpty();
    }

    @Test
    void applyOnHandDelta_increasesOnHand_whenPositive() {
        Long variantId = seedBalance("apply-positive", 5, 0);

        int rows = inventoryBalanceRepository.applyOnHandDelta(variantId, 3);

        assertThat(rows).isEqualTo(1);
        InventoryBalance after = inventoryBalanceRepository.findByProductVariantId(variantId).orElseThrow();
        assertThat(after.getOnHandQuantity()).isEqualTo(8);
    }

    @Test
    void applyOnHandDelta_rejectsNegativeDelta_wouldMakeOnHandNegative() {
        Long variantId = seedBalance("apply-negative-blocked", 2, 0);

        int rows = inventoryBalanceRepository.applyOnHandDelta(variantId, -5);

        assertThat(rows).isZero();
        InventoryBalance after = inventoryBalanceRepository.findByProductVariantId(variantId).orElseThrow();
        assertThat(after.getOnHandQuantity()).isEqualTo(2);
    }

    @Test
    void applyOnHandDelta_rejectsDelta_wouldMakeOnHandLessThanReserved() {
        Long variantId = seedBalance("apply-below-reserved", 5, 5);

        int rows = inventoryBalanceRepository.applyOnHandDelta(variantId, -1);

        assertThat(rows).isZero();
        InventoryBalance after = inventoryBalanceRepository.findByProductVariantId(variantId).orElseThrow();
        assertThat(after.getOnHandQuantity()).isEqualTo(5);
    }

    @Test
    void reserveQuantity_succeeds_whenAvailableIsEnough_thenFailsWhenNotEnough() {
        Long variantId = seedBalance("reserve", 5, 0);

        assertThat(inventoryBalanceRepository.reserveQuantity(variantId, 3)).isEqualTo(1);
        assertThat(inventoryBalanceRepository.reserveQuantity(variantId, 3)).isZero();

        InventoryBalance after = inventoryBalanceRepository.findByProductVariantId(variantId).orElseThrow();
        assertThat(after.getReservedQuantity()).isEqualTo(3);
    }

    @Test
    void releaseQuantity_succeeds_whenReservedIsEnough_thenFailsWhenNotEnough() {
        Long variantId = seedBalance("release", 5, 3);

        assertThat(inventoryBalanceRepository.releaseQuantity(variantId, 2)).isEqualTo(1);
        assertThat(inventoryBalanceRepository.releaseQuantity(variantId, 5)).isZero();

        InventoryBalance after = inventoryBalanceRepository.findByProductVariantId(variantId).orElseThrow();
        assertThat(after.getReservedQuantity()).isEqualTo(1);
    }

    @Test
    void fulfillQuantity_decreasesOnHandAndReserved_whenBothEnough() {
        Long variantId = seedBalance("fulfill-ok", 5, 3);

        int rows = inventoryBalanceRepository.fulfillQuantity(variantId, 3);

        assertThat(rows).isEqualTo(1);
        InventoryBalance after = inventoryBalanceRepository.findByProductVariantId(variantId).orElseThrow();
        assertThat(after.getOnHandQuantity()).isEqualTo(2);
        assertThat(after.getReservedQuantity()).isZero();
    }

    @Test
    void fulfillQuantity_fails_whenReservedNotEnough() {
        Long variantId = seedBalance("fulfill-blocked", 5, 1);

        int rows = inventoryBalanceRepository.fulfillQuantity(variantId, 3);

        assertThat(rows).isZero();
        InventoryBalance after = inventoryBalanceRepository.findByProductVariantId(variantId).orElseThrow();
        assertThat(after.getOnHandQuantity()).isEqualTo(5);
        assertThat(after.getReservedQuantity()).isEqualTo(1);
    }
}
