package com.laptophub.catalog.service;

import com.laptophub.catalog.dto.ProductVariantCreateRequest;
import com.laptophub.catalog.dto.ProductVariantUpdateRequest;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.entity.ProductVariantStatus;
import com.laptophub.catalog.repository.ProductVariantRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductVariantServiceTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductService productService;

    private ProductVariantService productVariantService;

    @BeforeEach
    void setUp() {
        productVariantService = new ProductVariantService(productVariantRepository, productService);
    }

    private ProductVariantCreateRequest createRequest() {
        return new ProductVariantCreateRequest("SKU-001", "16GB/512GB", new BigDecimal("999.00"), 16, 512, "SSD",
                "Black");
    }

    @Test
    void addVariant_savesVariant_whenProductExistsAndSkuUnique() {
        when(productService.getByIdOrThrow(1L)).thenReturn(Product.create(1L, 1L, "Laptop", "laptop", null, null));
        when(productVariantRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductVariant created = productVariantService.addVariant(1L, createRequest());

        assertThat(created.getProductId()).isEqualTo(1L);
        assertThat(created.getSku()).isEqualTo("SKU-001");
    }

    @Test
    void addVariant_throwsResourceNotFound_whenProductMissing() {
        when(productService.getByIdOrThrow(99L)).thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        assertThatThrownBy(() -> productVariantService.addVariant(99L, createRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void addVariant_throwsResourceConflict_whenSkuAlreadyExists() {
        when(productService.getByIdOrThrow(1L)).thenReturn(Product.create(1L, 1L, "Laptop", "laptop", null, null));
        when(productVariantRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productVariantService.addVariant(1L, createRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
    }

    @Test
    void updateVariant_appliesChanges_whenOwnedByProduct() {
        ProductVariant existing = ProductVariant.create(1L, "SKU-001", null, BigDecimal.TEN, null, null, null, null);
        when(productVariantRepository.findByIdAndProductId(5L, 1L)).thenReturn(Optional.of(existing));

        ProductVariant updated = productVariantService.updateVariant(1L, 5L,
                new ProductVariantUpdateRequest("8GB/256GB", new BigDecimal("799.00"), 8, 256, "SSD", "Silver"));

        assertThat(updated.getVariantName()).isEqualTo("8GB/256GB");
        assertThat(updated.getPrice()).isEqualByComparingTo("799.00");
    }

    @Test
    void updateVariant_throwsResourceNotFound_whenNotOwnedByProduct() {
        when(productVariantRepository.findByIdAndProductId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productVariantService.updateVariant(1L, 5L,
                new ProductVariantUpdateRequest(null, BigDecimal.TEN, null, null, null, null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void getByIdOrThrow_returnsVariant_whenExists() {
        ProductVariant variant = ProductVariant.create(1L, "SKU-001", null, BigDecimal.TEN, null, null, null, null);
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));

        assertThat(productVariantService.getByIdOrThrow(5L)).isSameAs(variant);
    }

    @Test
    void getByIdOrThrow_throwsResourceNotFound_whenMissing() {
        when(productVariantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productVariantService.getByIdOrThrow(99L)).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void activate_and_deactivate_toggleStatus() {
        ProductVariant variant = ProductVariant.create(1L, "SKU-001", null, BigDecimal.TEN, null, null, null, null);
        when(productVariantRepository.findByIdAndProductId(5L, 1L)).thenReturn(Optional.of(variant));

        ProductVariant deactivated = productVariantService.deactivate(1L, 5L);
        assertThat(deactivated.getStatus()).isEqualTo(ProductVariantStatus.INACTIVE);

        ProductVariant activated = productVariantService.activate(1L, 5L);
        assertThat(activated.getStatus()).isEqualTo(ProductVariantStatus.ACTIVE);
    }
}
