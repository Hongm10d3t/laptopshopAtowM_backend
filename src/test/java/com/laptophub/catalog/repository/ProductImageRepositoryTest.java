package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductImage;
import com.laptophub.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ProductImageRepositoryTest {

    @Autowired
    private ProductImageRepository productImageRepository;

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
    void findByProductIdOrderBySortOrderAsc_returnsImagesInOrder() {
        Long productId = newProductId("prod-imgorder");
        ProductImage second = productImageRepository.saveAndFlush(
                ProductImage.create(productId, "https://example.com/2.png", null, 2));
        ProductImage first = productImageRepository.saveAndFlush(
                ProductImage.create(productId, "https://example.com/1.png", null, 1));

        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);

        assertThat(images).extracting(ProductImage::getId).containsExactly(first.getId(), second.getId());
    }

    @Test
    void findByProductIdInOrderBySortOrderAsc_returnsImagesForMultipleProducts() {
        Long productId1 = newProductId("prod-imgbatch-1");
        Long productId2 = newProductId("prod-imgbatch-2");
        productImageRepository.saveAndFlush(ProductImage.create(productId1, "https://example.com/a.png", null, 0));
        productImageRepository.saveAndFlush(ProductImage.create(productId2, "https://example.com/b.png", null, 0));

        List<ProductImage> images = productImageRepository.findByProductIdInOrderBySortOrderAsc(
                List.of(productId1, productId2));

        assertThat(images).hasSize(2);
    }

    @Test
    void findByIdAndProductId_returnsEmpty_whenImageBelongsToAnotherProduct() {
        Long productId = newProductId("prod-imgowner");
        Long otherProductId = newProductId("prod-imgowner-other");
        ProductImage image = productImageRepository.saveAndFlush(
                ProductImage.create(productId, "https://example.com/a.png", null, 0));

        Optional<ProductImage> asOwner = productImageRepository.findByIdAndProductId(image.getId(), productId);
        Optional<ProductImage> asStranger = productImageRepository.findByIdAndProductId(image.getId(), otherProductId);

        assertThat(asOwner).isPresent();
        assertThat(asStranger).isEmpty();
    }
}
