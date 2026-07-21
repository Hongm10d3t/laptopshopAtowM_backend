package com.laptophub.catalog.service;

import com.laptophub.catalog.entity.SpecificationDefinition;
import com.laptophub.catalog.repository.SpecificationDefinitionRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecificationDefinitionServiceTest {

    @Mock
    private SpecificationDefinitionRepository specificationDefinitionRepository;

    private SpecificationDefinitionService specificationDefinitionService;

    @BeforeEach
    void setUp() {
        specificationDefinitionService = new SpecificationDefinitionService(specificationDefinitionRepository);
    }

    @Test
    void listAll_delegatesToRepository() {
        SpecificationDefinition cpu = SpecificationDefinition.create(null, "cpu", "CPU", null, null, 0);
        when(specificationDefinitionRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(cpu));

        assertThat(specificationDefinitionService.listAll()).containsExactly(cpu);
    }

    @Test
    void listForCategory_withNullCategoryId_returnsAll() {
        SpecificationDefinition cpu = SpecificationDefinition.create(null, "cpu", "CPU", null, null, 0);
        when(specificationDefinitionRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(cpu));

        assertThat(specificationDefinitionService.listForCategory(null)).containsExactly(cpu);
        verify(specificationDefinitionRepository, never()).findApplicableToCategory(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listForCategory_withCategoryId_delegatesToApplicableQuery() {
        SpecificationDefinition screenSize = SpecificationDefinition.create(1L, "screen_size", "Kich thuoc", "inch",
                null, 0);
        when(specificationDefinitionRepository.findApplicableToCategory(1L)).thenReturn(List.of(screenSize));

        assertThat(specificationDefinitionService.listForCategory(1L)).containsExactly(screenSize);
    }

    @Test
    void getByIdOrThrow_throwsResourceNotFound_whenMissing() {
        when(specificationDefinitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specificationDefinitionService.getByIdOrThrow(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void findByIds_returnsIdToDefinitionMap() {
        SpecificationDefinition cpu = SpecificationDefinition.create(null, "cpu", "CPU", null, null, 0);
        cpu.setId(1L);
        when(specificationDefinitionRepository.findAllById(List.of(1L))).thenReturn(List.of(cpu));

        var result = specificationDefinitionService.findByIds(List.of(1L));

        assertThat(result).containsEntry(1L, cpu);
    }
}
