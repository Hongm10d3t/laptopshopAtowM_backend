package com.laptophub.catalog.service;

import com.laptophub.catalog.dto.ProductSpecValuesUpsertRequest;
import com.laptophub.catalog.dto.ProductSpecValuesUpsertRequest.SpecValueItem;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductSpecValue;
import com.laptophub.catalog.entity.SpecificationDefinition;
import com.laptophub.catalog.repository.ProductSpecValueRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSpecValueServiceTest {

    @Mock
    private ProductSpecValueRepository productSpecValueRepository;

    @Mock
    private ProductService productService;

    @Mock
    private SpecificationDefinitionService specificationDefinitionService;

    private ProductSpecValueService productSpecValueService;

    @BeforeEach
    void setUp() {
        productSpecValueService = new ProductSpecValueService(productSpecValueRepository, productService,
                specificationDefinitionService);
    }

    private SpecificationDefinition cpuDefinition() {
        SpecificationDefinition definition = SpecificationDefinition.create(null, "cpu", "CPU", null, null, 0);
        definition.setId(1L);
        return definition;
    }

    @Test
    void upsertValues_createsNewValue_whenNoneExists() {
        when(productService.getByIdOrThrow(10L)).thenReturn(Product.create(1L, 1L, "Laptop", "laptop", null, null));
        SpecificationDefinition cpu = cpuDefinition();
        when(specificationDefinitionService.findByIds(List.of(1L))).thenReturn(Map.of(1L, cpu));
        when(productSpecValueRepository.findByProductId(10L)).thenReturn(List.of());
        when(productSpecValueRepository.save(any(ProductSpecValue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var responses = productSpecValueService.upsertValues(10L,
                new ProductSpecValuesUpsertRequest(List.of(new SpecValueItem(1L, "Intel Core i7"))));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).code()).isEqualTo("cpu");
        assertThat(responses.get(0).value()).isEqualTo("Intel Core i7");
        verify(productSpecValueRepository).deleteByProductIdAndSpecificationDefinitionIdNotIn(10L, List.of(1L));
    }

    @Test
    void upsertValues_updatesExistingValue_insteadOfCreatingDuplicate() {
        when(productService.getByIdOrThrow(10L)).thenReturn(Product.create(1L, 1L, "Laptop", "laptop", null, null));
        SpecificationDefinition cpu = cpuDefinition();
        when(specificationDefinitionService.findByIds(List.of(1L))).thenReturn(Map.of(1L, cpu));
        ProductSpecValue existing = ProductSpecValue.create(10L, 1L, "Intel Core i5");
        when(productSpecValueRepository.findByProductId(10L)).thenReturn(List.of(existing));

        var responses = productSpecValueService.upsertValues(10L,
                new ProductSpecValuesUpsertRequest(List.of(new SpecValueItem(1L, "Intel Core i7"))));

        assertThat(existing.getValue()).isEqualTo("Intel Core i7");
        assertThat(responses.get(0).value()).isEqualTo("Intel Core i7");
        verify(productSpecValueRepository, never()).save(any());
    }

    @Test
    void upsertValues_throwsResourceNotFound_whenSpecDefinitionMissing() {
        when(productService.getByIdOrThrow(10L)).thenReturn(Product.create(1L, 1L, "Laptop", "laptop", null, null));
        when(specificationDefinitionService.findByIds(List.of(99L))).thenReturn(Map.of());
        when(specificationDefinitionService.getByIdOrThrow(99L))
                .thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        assertThatThrownBy(() -> productSpecValueService.upsertValues(10L,
                new ProductSpecValuesUpsertRequest(List.of(new SpecValueItem(99L, "value")))))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void upsertValues_throwsResourceNotFound_whenProductMissing() {
        when(productService.getByIdOrThrow(99L)).thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        assertThatThrownBy(() -> productSpecValueService.upsertValues(99L,
                new ProductSpecValuesUpsertRequest(List.of(new SpecValueItem(1L, "value")))))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void upsertValues_emptyList_deletesAllExistingValues() {
        when(productService.getByIdOrThrow(10L)).thenReturn(Product.create(1L, 1L, "Laptop", "laptop", null, null));
        when(specificationDefinitionService.findByIds(List.of())).thenReturn(Map.of());
        ProductSpecValue existing = ProductSpecValue.create(10L, 1L, "Intel Core i5");
        when(productSpecValueRepository.findByProductId(10L)).thenReturn(List.of(existing));

        var responses = productSpecValueService.upsertValues(10L, new ProductSpecValuesUpsertRequest(List.of()));

        assertThat(responses).isEmpty();
        verify(productSpecValueRepository).deleteAll(List.of(existing));
        verify(productSpecValueRepository, never()).deleteByProductIdAndSpecificationDefinitionIdNotIn(any(), any());
    }

    @Test
    void listByProduct_returnsEnrichedResponses() {
        SpecificationDefinition cpu = cpuDefinition();
        ProductSpecValue value = ProductSpecValue.create(10L, 1L, "Intel Core i7");
        when(productSpecValueRepository.findByProductId(10L)).thenReturn(List.of(value));
        when(specificationDefinitionService.findByIds(List.of(1L))).thenReturn(Map.of(1L, cpu));

        var responses = productSpecValueService.listByProduct(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).code()).isEqualTo("cpu");
        assertThat(responses.get(0).value()).isEqualTo("Intel Core i7");
    }
}
