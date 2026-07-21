package com.laptophub.catalog.service;

import com.laptophub.catalog.dto.CategoryCreateRequest;
import com.laptophub.catalog.dto.CategoryUpdateRequest;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.CategoryStatus;
import com.laptophub.catalog.repository.CategoryRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
    }

    @Test
    void create_generatesSlugFromName_whenSlugBlank() {
        when(categoryRepository.existsBySlug("laptop-gaming")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category created = categoryService.create(new CategoryCreateRequest("Laptop Gaming", null, null));

        assertThat(created.getSlug()).isEqualTo("laptop-gaming");
    }

    @Test
    void create_normalizesRequestedSlug() {
        when(categoryRepository.existsBySlug("laptop-van-phong")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category created = categoryService.create(
                new CategoryCreateRequest("Laptop Văn Phòng", "Laptop Văn Phòng", null));

        assertThat(created.getSlug()).isEqualTo("laptop-van-phong");
    }

    @Test
    void create_throwsResourceConflict_whenSlugAlreadyExists() {
        when(categoryRepository.existsBySlug("laptop")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryCreateRequest("Laptop", "laptop", null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
    }

    @Test
    void update_appliesChanges_whenSlugNotTaken() {
        Category existing = Category.create("Laptop", "laptop", null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsBySlugAndIdNot("laptop-gaming", 1L)).thenReturn(false);

        Category updated = categoryService.update(1L,
                new CategoryUpdateRequest("Laptop Gaming", "laptop-gaming", "Mo ta moi"));

        assertThat(updated.getName()).isEqualTo("Laptop Gaming");
        assertThat(updated.getSlug()).isEqualTo("laptop-gaming");
        assertThat(updated.getDescription()).isEqualTo("Mo ta moi");
    }

    @Test
    void update_throwsResourceConflict_whenSlugTakenByAnotherCategory() {
        Category existing = Category.create("Laptop", "laptop", null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsBySlugAndIdNot("chuot", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, new CategoryUpdateRequest("Chuot", "chuot", null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
    }

    @Test
    void update_throwsResourceNotFound_whenCategoryMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(99L, new CategoryUpdateRequest("Laptop", "laptop", null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void activate_and_deactivate_toggleStatus() {
        Category existing = Category.create("Laptop", "laptop", null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        Category deactivated = categoryService.deactivate(1L);
        assertThat(deactivated.getStatus()).isEqualTo(CategoryStatus.INACTIVE);

        Category activated = categoryService.activate(1L);
        assertThat(activated.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
    }

    @Test
    void getByIdOrThrow_throwsResourceNotFound_whenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getByIdOrThrow(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
