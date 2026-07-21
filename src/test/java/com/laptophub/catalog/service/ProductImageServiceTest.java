package com.laptophub.catalog.service;

import com.laptophub.catalog.dto.ProductImageCreateRequest;
import com.laptophub.catalog.dto.ProductImageReorderRequest;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductImage;
import com.laptophub.catalog.repository.ProductImageRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductService productService;

    private ProductImageService productImageService;

    @BeforeEach
    void setUp() {
        productImageService = new ProductImageService(productImageRepository, productService);
    }

    @Test
    void addImage_usesProvidedSortOrder_whenGiven() {
        when(productService.getByIdOrThrow(1L)).thenReturn(Product.create(1L, 1L, "Laptop", "laptop", null, null));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductImage image = productImageService.addImage(1L,
                new ProductImageCreateRequest("https://example.com/a.png", "Anh", 5));

        assertThat(image.getSortOrder()).isEqualTo(5);
    }

    @Test
    void addImage_appendsToEnd_whenSortOrderNotProvided() {
        when(productService.getByIdOrThrow(1L)).thenReturn(Product.create(1L, 1L, "Laptop", "laptop", null, null));
        ProductImage existing = ProductImage.create(1L, "https://example.com/existing.png", null, 2);
        when(productImageRepository.findByProductIdOrderBySortOrderAsc(1L)).thenReturn(List.of(existing));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductImage image = productImageService.addImage(1L,
                new ProductImageCreateRequest("https://example.com/new.png", null, null));

        assertThat(image.getSortOrder()).isEqualTo(3);
    }

    @Test
    void addImage_firstImage_startsAtZero_whenSortOrderNotProvided() {
        when(productService.getByIdOrThrow(1L)).thenReturn(Product.create(1L, 1L, "Laptop", "laptop", null, null));
        when(productImageRepository.findByProductIdOrderBySortOrderAsc(1L)).thenReturn(List.of());
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductImage image = productImageService.addImage(1L,
                new ProductImageCreateRequest("https://example.com/first.png", null, null));

        assertThat(image.getSortOrder()).isZero();
    }

    @Test
    void removeImage_deletesOwnedImage() {
        ProductImage image = ProductImage.create(1L, "https://example.com/a.png", null, 0);
        when(productImageRepository.findByIdAndProductId(5L, 1L)).thenReturn(Optional.of(image));

        productImageService.removeImage(1L, 5L);

        verify(productImageRepository).delete(image);
    }

    @Test
    void removeImage_throwsResourceNotFound_whenNotOwned() {
        when(productImageRepository.findByIdAndProductId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.removeImage(1L, 5L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void reorderImages_assignsSortOrderByRequestedPosition() {
        ProductImage imageA = ProductImage.create(1L, "https://example.com/a.png", null, 0);
        ProductImage imageB = ProductImage.create(1L, "https://example.com/b.png", null, 1);
        when(productImageRepository.findByIdAndProductId(10L, 1L)).thenReturn(Optional.of(imageA));
        when(productImageRepository.findByIdAndProductId(20L, 1L)).thenReturn(Optional.of(imageB));

        productImageService.reorderImages(1L, new ProductImageReorderRequest(List.of(20L, 10L)));

        assertThat(imageB.getSortOrder()).isZero();
        assertThat(imageA.getSortOrder()).isEqualTo(1);
    }
}
