package com.laptophub.catalog.service;

import com.laptophub.catalog.dto.BrandCreateRequest;
import com.laptophub.catalog.dto.BrandUpdateRequest;
import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.BrandStatus;
import com.laptophub.catalog.repository.BrandRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    private BrandService brandService;

    @BeforeEach
    void setUp() {
        brandService = new BrandService(brandRepository);
    }

    @Test
    void create_generatesSlugFromName_whenSlugBlank() {
        when(brandRepository.existsBySlug("asus")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Brand created = brandService.create(new BrandCreateRequest("Asus", null, null, null));

        assertThat(created.getSlug()).isEqualTo("asus");
    }

    @Test
    void create_throwsResourceConflict_whenSlugAlreadyExists() {
        when(brandRepository.existsBySlug("asus")).thenReturn(true);

        assertThatThrownBy(() -> brandService.create(new BrandCreateRequest("Asus", "asus", null, null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
    }

    @Test
    void update_appliesChanges_whenSlugNotTaken() {
        Brand existing = Brand.create("Asus", "asus", null, null);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepository.existsBySlugAndIdNot("dell", 1L)).thenReturn(false);

        Brand updated = brandService.update(1L,
                new BrandUpdateRequest("Dell", "dell", "Mo ta moi", "https://example.com/dell.png"));

        assertThat(updated.getName()).isEqualTo("Dell");
        assertThat(updated.getSlug()).isEqualTo("dell");
        assertThat(updated.getDescription()).isEqualTo("Mo ta moi");
        assertThat(updated.getLogoUrl()).isEqualTo("https://example.com/dell.png");
    }

    @Test
    void update_throwsResourceConflict_whenSlugTakenByAnotherBrand() {
        Brand existing = Brand.create("Asus", "asus", null, null);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepository.existsBySlugAndIdNot("dell", 1L)).thenReturn(true);

        assertThatThrownBy(() -> brandService.update(1L, new BrandUpdateRequest("Dell", "dell", null, null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
    }

    @Test
    void update_throwsResourceNotFound_whenBrandMissing() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.update(99L, new BrandUpdateRequest("Asus", "asus", null, null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void activate_and_deactivate_toggleStatus() {
        Brand existing = Brand.create("Asus", "asus", null, null);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(existing));

        Brand deactivated = brandService.deactivate(1L);
        assertThat(deactivated.getStatus()).isEqualTo(BrandStatus.INACTIVE);

        Brand activated = brandService.activate(1L);
        assertThat(activated.getStatus()).isEqualTo(BrandStatus.ACTIVE);
    }

    @Test
    void getByIdOrThrow_throwsResourceNotFound_whenMissing() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.getByIdOrThrow(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void findNamesByIds_returnsIdToNameMap() {
        Brand asus = Brand.create("Asus", "asus", null, null);
        asus.setId(1L);
        Brand dell = Brand.create("Dell", "dell", null, null);
        dell.setId(2L);
        when(brandRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(asus, dell));

        var names = brandService.findNamesByIds(List.of(1L, 2L));

        assertThat(names).containsEntry(1L, "Asus").containsEntry(2L, "Dell");
    }
}
