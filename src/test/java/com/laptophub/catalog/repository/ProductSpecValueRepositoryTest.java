package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductSpecValue;
import com.laptophub.catalog.entity.SpecificationDefinition;
import com.laptophub.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ProductSpecValueRepositoryTest {

    @Autowired
    private ProductSpecValueRepository productSpecValueRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private SpecificationDefinitionRepository specificationDefinitionRepository;

    private Long newProductId(String slug) {
        Long categoryId = categoryRepository.saveAndFlush(Category.create("Cat " + slug, "cat-" + slug, null)).getId();
        Long brandId = brandRepository.saveAndFlush(Brand.create("Brand " + slug, "brand-" + slug, null, null)).getId();
        return productRepository.saveAndFlush(Product.create(categoryId, brandId, "Product " + slug, slug, null, null))
                .getId();
    }

    private Long newSpecId(String code) {
        return specificationDefinitionRepository.saveAndFlush(
                SpecificationDefinition.create(null, code, "Label " + code, null, null, 0)).getId();
    }

    @Test
    void findByProductId_returnsAllValuesOfProduct() {
        Long productId = newProductId("prod-specvalues");
        Long specId1 = newSpecId("spec-code-1");
        Long specId2 = newSpecId("spec-code-2");
        productSpecValueRepository.saveAndFlush(ProductSpecValue.create(productId, specId1, "Gia tri 1"));
        productSpecValueRepository.saveAndFlush(ProductSpecValue.create(productId, specId2, "Gia tri 2"));

        List<ProductSpecValue> values = productSpecValueRepository.findByProductId(productId);

        assertThat(values).hasSize(2);
    }

    @Test
    void savingDuplicateProductAndSpecification_violatesUniqueConstraint() {
        Long productId = newProductId("prod-specvalues-dup");
        Long specId = newSpecId("spec-code-dup");
        productSpecValueRepository.saveAndFlush(ProductSpecValue.create(productId, specId, "Gia tri"));
        ProductSpecValue duplicate = ProductSpecValue.create(productId, specId, "Gia tri khac");

        assertThatThrownBy(() -> productSpecValueRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteByProductIdAndSpecificationDefinitionIdNotIn_removesValuesNotInList() {
        Long productId = newProductId("prod-specvalues-delete");
        Long specIdKeep = newSpecId("spec-code-keep");
        Long specIdRemove = newSpecId("spec-code-remove");
        productSpecValueRepository.saveAndFlush(ProductSpecValue.create(productId, specIdKeep, "Giu lai"));
        productSpecValueRepository.saveAndFlush(ProductSpecValue.create(productId, specIdRemove, "Bi xoa"));

        productSpecValueRepository.deleteByProductIdAndSpecificationDefinitionIdNotIn(productId, List.of(specIdKeep));

        List<ProductSpecValue> remaining = productSpecValueRepository.findByProductId(productId);
        assertThat(remaining).extracting(ProductSpecValue::getSpecificationDefinitionId).containsExactly(specIdKeep);
    }
}
