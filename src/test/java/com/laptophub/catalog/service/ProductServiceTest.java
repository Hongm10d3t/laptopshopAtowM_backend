package com.laptophub.catalog.service;

import com.laptophub.catalog.dto.ProductCreateRequest;
import com.laptophub.catalog.dto.ProductSummaryResponse;
import com.laptophub.catalog.dto.ProductUpdateRequest;
import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private BrandService brandService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryService, brandService);
    }

    private Category activeCategory(long id) {
        Category category = Category.create("Laptop", "laptop", null);
        category.setId(id);
        return category;
    }

    private Brand activeBrand(long id) {
        Brand brand = Brand.create("Asus", "asus", null, null);
        brand.setId(id);
        return brand;
    }

    @Test
    void create_savesProduct_whenCategoryAndBrandActive() {
        when(categoryService.getByIdOrThrow(1L)).thenReturn(activeCategory(1L));
        when(brandService.getByIdOrThrow(2L)).thenReturn(activeBrand(2L));
        when(productRepository.existsBySlug("laptop-gaming")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = productService.create(new ProductCreateRequest(1L, 2L, "Laptop Gaming", null, null, null));

        assertThat(created.getSlug()).isEqualTo("laptop-gaming");
        assertThat(created.getCategoryId()).isEqualTo(1L);
        assertThat(created.getBrandId()).isEqualTo(2L);
    }

    @Test
    void create_throwsValidationError_whenCategoryInactive() {
        Category inactiveCategory = activeCategory(1L);
        inactiveCategory.deactivate();
        when(categoryService.getByIdOrThrow(1L)).thenReturn(inactiveCategory);

        assertThatThrownBy(() -> productService.create(new ProductCreateRequest(1L, 2L, "Laptop", null, null, null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void create_throwsValidationError_whenBrandInactive() {
        when(categoryService.getByIdOrThrow(1L)).thenReturn(activeCategory(1L));
        Brand inactiveBrand = activeBrand(2L);
        inactiveBrand.deactivate();
        when(brandService.getByIdOrThrow(2L)).thenReturn(inactiveBrand);

        assertThatThrownBy(() -> productService.create(new ProductCreateRequest(1L, 2L, "Laptop", null, null, null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void create_throwsResourceConflict_whenSlugAlreadyExists() {
        when(categoryService.getByIdOrThrow(1L)).thenReturn(activeCategory(1L));
        when(brandService.getByIdOrThrow(2L)).thenReturn(activeBrand(2L));
        when(productRepository.existsBySlug("laptop")).thenReturn(true);

        assertThatThrownBy(() ->
                productService.create(new ProductCreateRequest(1L, 2L, "Laptop", "laptop", null, null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
    }

    @Test
    void update_appliesChanges_whenValid() {
        Product existing = Product.create(1L, 2L, "Laptop", "laptop", null, null);
        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryService.getByIdOrThrow(3L)).thenReturn(activeCategory(3L));
        when(brandService.getByIdOrThrow(4L)).thenReturn(activeBrand(4L));
        when(productRepository.existsBySlugAndIdNot("laptop-moi", 10L)).thenReturn(false);

        Product updated = productService.update(10L,
                new ProductUpdateRequest(3L, 4L, "Laptop Moi", "laptop-moi", "Ngan", "Dai"));

        assertThat(updated.getName()).isEqualTo("Laptop Moi");
        assertThat(updated.getCategoryId()).isEqualTo(3L);
        assertThat(updated.getBrandId()).isEqualTo(4L);
    }

    @Test
    void update_throwsResourceNotFound_whenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.update(99L, new ProductUpdateRequest(1L, 2L, "Laptop", "laptop", null, null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void getByIdOrThrow_throwsResourceNotFound_whenMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getByIdOrThrow(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void listAdmin_enrichesCategoryAndBrandNames() {
        Product product = Product.create(1L, 2L, "Laptop", "laptop", null, null);
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.searchAdmin(null, null, null, null, PageRequest.of(0, 20))).thenReturn(page);
        when(categoryService.findNamesByIds(List.of(1L))).thenReturn(Map.of(1L, "Laptop Category"));
        when(brandService.findNamesByIds(List.of(2L))).thenReturn(Map.of(2L, "Asus"));

        Page<ProductSummaryResponse> result = productService.listAdmin(null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).categoryName()).isEqualTo("Laptop Category");
        assertThat(result.getContent().get(0).brandName()).isEqualTo("Asus");
    }

    @Test
    void activate_and_deactivate_toggleStatus() {
        Product product = Product.create(1L, 2L, "Laptop", "laptop", null, null);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        var deactivated = productService.deactivate(10L);
        assertThat(deactivated.getStatus()).isEqualTo(com.laptophub.catalog.entity.ProductStatus.INACTIVE);

        var activated = productService.activate(10L);
        assertThat(activated.getStatus()).isEqualTo(com.laptophub.catalog.entity.ProductStatus.ACTIVE);
    }
}
