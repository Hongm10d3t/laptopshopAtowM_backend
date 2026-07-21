package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.entity.ProductVariantStatus;
import com.laptophub.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ProductVariantRepositoryTest {

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    private Long newProductId(String slug) {
        Long categoryId = categoryRepository.saveAndFlush(Category.create("Cat " + slug, "cat-" + slug, null)).getId();
        Long brandId = brandRepository.saveAndFlush(Brand.create("Brand " + slug, "brand-" + slug, null, null)).getId();
        return productRepository.saveAndFlush(Product.create(categoryId, brandId, "Product " + slug, slug, null, null))
                .getId();
    }

    @Test
    void findByProductId_returnsAllVariantsOfProduct() {
        Long productId = newProductId("prod-findbyproduct");
        productVariantRepository.saveAndFlush(
                ProductVariant.create(productId, "SKU-FBP-1", null, BigDecimal.TEN, null, null, null, null));
        productVariantRepository.saveAndFlush(
                ProductVariant.create(productId, "SKU-FBP-2", null, BigDecimal.ONE, null, null, null, null));

        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);

        assertThat(variants).hasSize(2);
    }

    @Test
    void findByIdAndProductId_returnsEmpty_whenVariantBelongsToAnotherProduct() {
        Long productId = newProductId("prod-ownercheck");
        Long otherProductId = newProductId("prod-ownercheck-other");
        ProductVariant variant = productVariantRepository.saveAndFlush(
                ProductVariant.create(productId, "SKU-OWNER-1", null, BigDecimal.TEN, null, null, null, null));

        assertThat(productVariantRepository.findByIdAndProductId(variant.getId(), productId)).isPresent();
        assertThat(productVariantRepository.findByIdAndProductId(variant.getId(), otherProductId)).isEmpty();
    }

    @Test
    void existsBySkuAndIdNot_excludesGivenId() {
        Long productId = newProductId("prod-existsbysku");
        ProductVariant saved = productVariantRepository.saveAndFlush(
                ProductVariant.create(productId, "SKU-EXISTS-1", null, BigDecimal.TEN, null, null, null, null));

        assertThat(productVariantRepository.existsBySkuAndIdNot("SKU-EXISTS-1", saved.getId())).isFalse();
        assertThat(productVariantRepository.existsBySkuAndIdNot("SKU-EXISTS-1", saved.getId() + 1)).isTrue();
    }

    @Test
    void savingDuplicateSku_violatesUniqueConstraint() {
        Long productId = newProductId("prod-dupsku");
        productVariantRepository.saveAndFlush(
                ProductVariant.create(productId, "SKU-DUP-1", null, BigDecimal.TEN, null, null, null, null));
        ProductVariant duplicate = ProductVariant.create(productId, "SKU-DUP-1", null, BigDecimal.ONE, null, null,
                null, null);

        assertThatThrownBy(() -> productVariantRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findMinMaxPriceByProductIdIn_returnsRangePerProduct_onlyActiveVariants() {
        Long productId = newProductId("prod-pricerange");
        productVariantRepository.saveAndFlush(
                ProductVariant.create(productId, "SKU-PR-1", null, new BigDecimal("500.00"), null, null, null, null));
        productVariantRepository.saveAndFlush(
                ProductVariant.create(productId, "SKU-PR-2", null, new BigDecimal("900.00"), null, null, null, null));
        ProductVariant inactiveCheap = ProductVariant.create(productId, "SKU-PR-3", null, new BigDecimal("100.00"),
                null, null, null, null);
        inactiveCheap.deactivate();
        productVariantRepository.saveAndFlush(inactiveCheap);

        List<ProductPriceRange> ranges = productVariantRepository.findMinMaxPriceByProductIdIn(List.of(productId));

        assertThat(ranges).hasSize(1);
        ProductPriceRange range = ranges.get(0);
        assertThat(range.getProductId()).isEqualTo(productId);
        assertThat(range.getMinPrice()).isEqualByComparingTo("500.00");
        assertThat(range.getMaxPrice()).isEqualByComparingTo("900.00");
    }

    @Test
    void findByProductIdAndStatus_filtersByStatus() {
        Long productId = newProductId("prod-bystatus");
        ProductVariant active = productVariantRepository.saveAndFlush(
                ProductVariant.create(productId, "SKU-STATUS-1", null, BigDecimal.TEN, null, null, null, null));
        ProductVariant inactive = ProductVariant.create(productId, "SKU-STATUS-2", null, BigDecimal.ONE, null, null,
                null, null);
        inactive.deactivate();
        productVariantRepository.saveAndFlush(inactive);

        List<ProductVariant> activeOnly = productVariantRepository.findByProductIdAndStatus(productId,
                ProductVariantStatus.ACTIVE);

        assertThat(activeOnly).extracting(ProductVariant::getId).containsExactly(active.getId());
    }
}
