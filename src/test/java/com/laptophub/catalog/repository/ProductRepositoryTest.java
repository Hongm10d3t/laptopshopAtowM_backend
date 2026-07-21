package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductStatus;
import com.laptophub.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    private Long newCategoryId(String slug) {
        return categoryRepository.saveAndFlush(Category.create("Cat " + slug, slug, null)).getId();
    }

    private Long newBrandId(String slug) {
        return brandRepository.saveAndFlush(Brand.create("Brand " + slug, slug, null, null)).getId();
    }

    @Test
    void findBySlug_returnsProduct_whenExists() {
        Long categoryId = newCategoryId("cat-findbyslug");
        Long brandId = newBrandId("brand-findbyslug");
        productRepository.saveAndFlush(
                Product.create(categoryId, brandId, "Laptop A", "laptop-a-findbyslug", null, null));

        Optional<Product> found = productRepository.findBySlug("laptop-a-findbyslug");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Laptop A");
    }

    @Test
    void existsBySlugAndIdNot_excludesGivenId() {
        Long categoryId = newCategoryId("cat-existsbyslug");
        Long brandId = newBrandId("brand-existsbyslug");
        Product saved = productRepository.saveAndFlush(
                Product.create(categoryId, brandId, "Laptop B", "laptop-b-existsbyslug", null, null));

        assertThat(productRepository.existsBySlugAndIdNot("laptop-b-existsbyslug", saved.getId())).isFalse();
        assertThat(productRepository.existsBySlugAndIdNot("laptop-b-existsbyslug", saved.getId() + 1)).isTrue();
    }

    @Test
    void savingDuplicateSlug_violatesUniqueConstraint() {
        Long categoryId = newCategoryId("cat-dup");
        Long brandId = newBrandId("brand-dup");
        productRepository.saveAndFlush(Product.create(categoryId, brandId, "Laptop C", "laptop-c-dup", null, null));
        Product duplicate = Product.create(categoryId, brandId, "Laptop C Khac", "laptop-c-dup", null, null);

        assertThatThrownBy(() -> productRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void searchAdmin_filtersByCategoryBrandStatusAndKeyword() {
        Long categoryId = newCategoryId("cat-search");
        Long otherCategoryId = newCategoryId("cat-search-other");
        Long brandId = newBrandId("brand-search");
        Product matching = productRepository.saveAndFlush(
                Product.create(categoryId, brandId, "Laptop Gaming Search", "laptop-gaming-search", null, null));
        Product wrongCategory = productRepository.saveAndFlush(
                Product.create(otherCategoryId, brandId, "Laptop Gaming Other", "laptop-gaming-other", null, null));
        Product inactive = productRepository.saveAndFlush(
                Product.create(categoryId, brandId, "Laptop Gaming Inactive", "laptop-gaming-inactive", null, null));
        inactive.deactivate();
        productRepository.saveAndFlush(inactive);

        var page = productRepository.searchAdmin(categoryId, brandId, ProductStatus.ACTIVE, "gaming",
                PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Product::getId).containsExactly(matching.getId());
        assertThat(page.getContent()).extracting(Product::getId).doesNotContain(wrongCategory.getId(), inactive.getId());
    }

    @Test
    void searchAdmin_withAllFiltersNull_returnsEverything() {
        Long categoryId = newCategoryId("cat-search-all");
        Long brandId = newBrandId("brand-search-all");
        productRepository.saveAndFlush(
                Product.create(categoryId, brandId, "Laptop Search All", "laptop-search-all", null, null));

        var page = productRepository.searchAdmin(null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).isNotEmpty();
    }
}
