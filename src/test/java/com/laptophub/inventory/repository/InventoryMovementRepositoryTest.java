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
import com.laptophub.inventory.entity.InventoryMovement;
import com.laptophub.inventory.entity.InventoryMovementType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class InventoryMovementRepositoryTest {

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

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

    @Test
    void findByProductVariantIdOrderByCreatedAtDesc_returnsNewestFirst() {
        Long variantId = newVariantId("movement-order");
        inventoryMovementRepository.saveAndFlush(InventoryMovement.create(variantId,
                InventoryMovementType.RECEIPT, 5, 5, 0, "STOCK_RECEIPT", 1L, null, null));
        inventoryMovementRepository.saveAndFlush(InventoryMovement.create(variantId,
                InventoryMovementType.ADJUSTMENT_OUT, 2, 3, 0, null, null, "Hàng hỏng", null));

        Page<InventoryMovement> page = inventoryMovementRepository
                .findByProductVariantIdOrderByCreatedAtDesc(variantId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getType()).isEqualTo(InventoryMovementType.ADJUSTMENT_OUT);
        assertThat(page.getContent().get(1).getType()).isEqualTo(InventoryMovementType.RECEIPT);
    }

    @Test
    void findByProductVariantIdAndOptionalType_filtersByType_whenProvided() {
        Long variantId = newVariantId("movement-filter");
        inventoryMovementRepository.saveAndFlush(InventoryMovement.create(variantId,
                InventoryMovementType.RECEIPT, 5, 5, 0, "STOCK_RECEIPT", 1L, null, null));
        inventoryMovementRepository.saveAndFlush(InventoryMovement.create(variantId,
                InventoryMovementType.RESERVE, 2, 5, 2, null, null, null, null));

        Page<InventoryMovement> filtered = inventoryMovementRepository.findByProductVariantIdAndOptionalType(
                variantId, InventoryMovementType.RESERVE, PageRequest.of(0, 10));
        Page<InventoryMovement> unfiltered = inventoryMovementRepository.findByProductVariantIdAndOptionalType(
                variantId, null, PageRequest.of(0, 10));

        assertThat(filtered.getContent()).extracting(InventoryMovement::getType)
                .containsExactly(InventoryMovementType.RESERVE);
        assertThat(unfiltered.getContent()).hasSize(2);
    }
}
