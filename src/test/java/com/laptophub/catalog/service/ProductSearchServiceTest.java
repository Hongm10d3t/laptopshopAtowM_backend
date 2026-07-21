package com.laptophub.catalog.service;

import com.laptophub.catalog.dto.ProductDetailResponse;
import com.laptophub.catalog.dto.ProductListItemResponse;
import com.laptophub.catalog.dto.ProductSortOption;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductImage;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.entity.ProductVariantStatus;
import com.laptophub.catalog.repository.ProductImageRepository;
import com.laptophub.catalog.repository.ProductPriceRange;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.catalog.repository.ProductVariantRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private BrandService brandService;

    @Mock
    private ProductSpecValueService productSpecValueService;

    private ProductSearchService productSearchService;

    @BeforeEach
    void setUp() {
        productSearchService = new ProductSearchService(productRepository, productVariantRepository,
                productImageRepository, categoryService, brandService, productSpecValueService);
    }

    private Product product(long id, String name, java.time.Instant createdAt) {
        Product product = Product.create(1L, 2L, name, "slug-" + id, null, null);
        product.setId(id);
        product.setCreatedAt(createdAt);
        return product;
    }

    private ProductPriceRange priceRange(long productId, BigDecimal min, BigDecimal max) {
        return new ProductPriceRange() {
            public Long getProductId() {
                return productId;
            }

            public BigDecimal getMinPrice() {
                return min;
            }

            public BigDecimal getMaxPrice() {
                return max;
            }
        };
    }

    @Test
    void search_returnsEmptyPage_whenNoMatches() {
        when(productRepository.searchPublic(null, null, null, null, null)).thenReturn(List.of());

        Page<ProductListItemResponse> result = productSearchService.search(null, null, null, null, null,
                ProductSortOption.NEWEST, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void search_sortsByNameAscAndEnrichesNamesAndThumbnail() {
        Product productB = product(2L, "Banana Laptop", java.time.Instant.now());
        Product productA = product(1L, "Apple Laptop", java.time.Instant.now());
        when(productRepository.searchPublic(null, null, null, null, null)).thenReturn(List.of(productB, productA));
        when(productVariantRepository.findMinMaxPriceByProductIdIn(List.of(2L, 1L))).thenReturn(List.of(
                priceRange(2L, new BigDecimal("500"), new BigDecimal("600")),
                priceRange(1L, new BigDecimal("300"), new BigDecimal("400"))));
        when(categoryService.findNamesByIds(List.of(1L))).thenReturn(Map.of(1L, "Laptop"));
        when(brandService.findNamesByIds(List.of(2L))).thenReturn(Map.of(2L, "Asus"));
        ProductImage thumbnail = ProductImage.create(1L, "https://example.com/apple.png", null, 0);
        when(productImageRepository.findByProductIdInOrderBySortOrderAsc(List.of(1L, 2L)))
                .thenReturn(List.of(thumbnail));

        Page<ProductListItemResponse> result = productSearchService.search(null, null, null, null, null,
                ProductSortOption.NAME_ASC, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(ProductListItemResponse::name)
                .containsExactly("Apple Laptop", "Banana Laptop");
        assertThat(result.getContent().get(0).categoryName()).isEqualTo("Laptop");
        assertThat(result.getContent().get(0).brandName()).isEqualTo("Asus");
        assertThat(result.getContent().get(0).thumbnailUrl()).isEqualTo("https://example.com/apple.png");
        assertThat(result.getContent().get(0).priceFrom()).isEqualByComparingTo("300");
    }

    @Test
    void search_sortsByPriceAsc() {
        Product expensive = product(1L, "Expensive", java.time.Instant.now());
        Product cheap = product(2L, "Cheap", java.time.Instant.now());
        when(productRepository.searchPublic(null, null, null, null, null)).thenReturn(List.of(expensive, cheap));
        when(productVariantRepository.findMinMaxPriceByProductIdIn(List.of(1L, 2L))).thenReturn(List.of(
                priceRange(1L, new BigDecimal("2000"), new BigDecimal("2000")),
                priceRange(2L, new BigDecimal("500"), new BigDecimal("500"))));
        when(categoryService.findNamesByIds(List.of(1L))).thenReturn(Map.of());
        when(brandService.findNamesByIds(List.of(2L))).thenReturn(Map.of());
        when(productImageRepository.findByProductIdInOrderBySortOrderAsc(List.of(2L, 1L))).thenReturn(List.of());

        Page<ProductListItemResponse> result = productSearchService.search(null, null, null, null, null,
                ProductSortOption.PRICE_ASC, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(ProductListItemResponse::name).containsExactly("Cheap", "Expensive");
    }

    @Test
    void search_paginatesManually_returnsCorrectSecondPage() {
        Product first = product(1L, "A", java.time.Instant.now());
        Product second = product(2L, "B", java.time.Instant.now());
        Product third = product(3L, "C", java.time.Instant.now());
        when(productRepository.searchPublic(null, null, null, null, null)).thenReturn(List.of(first, second, third));
        when(productVariantRepository.findMinMaxPriceByProductIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(
                priceRange(1L, BigDecimal.ONE, BigDecimal.ONE),
                priceRange(2L, BigDecimal.ONE, BigDecimal.ONE),
                priceRange(3L, BigDecimal.ONE, BigDecimal.ONE)));
        when(categoryService.findNamesByIds(List.of(1L))).thenReturn(Map.of());
        when(brandService.findNamesByIds(List.of(2L))).thenReturn(Map.of());
        when(productImageRepository.findByProductIdInOrderBySortOrderAsc(List.of(3L))).thenReturn(List.of());

        Pageable secondPageOfOne = PageRequest.of(2, 1);
        Page<ProductListItemResponse> result = productSearchService.search(null, null, null, null, null,
                ProductSortOption.NAME_ASC, secondPageOfOne);

        assertThat(result.getContent()).extracting(ProductListItemResponse::name).containsExactly("C");
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void getDetailBySlug_returnsActiveVariantsImagesAndSpecs() {
        Product product = Product.create(1L, 2L, "Laptop", "laptop", "Ngan", "Dai");
        product.setId(10L);
        when(productRepository.findPublicBySlug("laptop")).thenReturn(Optional.of(product));
        when(categoryService.getByIdOrThrow(1L)).thenReturn(categoryNamed("Laptop"));
        when(brandService.getByIdOrThrow(2L)).thenReturn(brandNamed("Asus"));
        ProductVariant variant = ProductVariant.create(10L, "SKU-1", null, BigDecimal.TEN, null, null, null, null);
        when(productVariantRepository.findByProductIdAndStatus(10L, ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant));
        ProductImage image = ProductImage.create(10L, "https://example.com/a.png", null, 0);
        when(productImageRepository.findByProductIdOrderBySortOrderAsc(10L)).thenReturn(List.of(image));
        when(productSpecValueService.listByProduct(10L)).thenReturn(List.of());

        ProductDetailResponse detail = productSearchService.getDetailBySlug("laptop");

        assertThat(detail.name()).isEqualTo("Laptop");
        assertThat(detail.categoryName()).isEqualTo("Laptop");
        assertThat(detail.brandName()).isEqualTo("Asus");
        assertThat(detail.variants()).hasSize(1);
        assertThat(detail.images()).hasSize(1);
    }

    @Test
    void getDetailBySlug_throwsResourceNotFound_whenMissing() {
        when(productRepository.findPublicBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSearchService.getDetailBySlug("missing"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private com.laptophub.catalog.entity.Category categoryNamed(String name) {
        return com.laptophub.catalog.entity.Category.create(name, "slug-" + name.toLowerCase(), null);
    }

    private com.laptophub.catalog.entity.Brand brandNamed(String name) {
        return com.laptophub.catalog.entity.Brand.create(name, "slug-" + name.toLowerCase(), null, null);
    }
}
