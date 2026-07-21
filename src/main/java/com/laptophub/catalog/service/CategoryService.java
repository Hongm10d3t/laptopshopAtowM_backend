package com.laptophub.catalog.service;

import com.laptophub.catalog.SlugGenerator;
import com.laptophub.catalog.dto.CategoryCreateRequest;
import com.laptophub.catalog.dto.CategoryUpdateRequest;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.CategoryStatus;
import com.laptophub.catalog.repository.CategoryRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category create(CategoryCreateRequest request) {
        String slug = resolveSlug(request.slug(), request.name());
        if (categoryRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Slug đã tồn tại");
        }
        return categoryRepository.save(Category.create(request.name(), slug, request.description()));
    }

    @Transactional
    public Category update(Long id, CategoryUpdateRequest request) {
        Category category = getByIdOrThrow(id);
        String slug = resolveSlug(request.slug(), request.name());
        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Slug đã tồn tại");
        }
        category.update(request.name(), slug, request.description());
        return category;
    }

    public Category getByIdOrThrow(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public Page<Category> list(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    // Dùng bởi PublicCategoryController — chỉ danh mục đang hiển thị.
    public List<Category> listActive() {
        return categoryRepository.findByStatusOrderByNameAsc(CategoryStatus.ACTIVE);
    }

    // Batch fetch tên danh mục theo id — dùng bởi ProductService khi lắp
    // danh sách sản phẩm, tránh N+1 (1 query duy nhất thay vì 1 query/dòng).
    public Map<Long, String> findNamesByIds(List<Long> ids) {
        return categoryRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    @Transactional
    public Category activate(Long id) {
        Category category = getByIdOrThrow(id);
        category.activate();
        return category;
    }

    @Transactional
    public Category deactivate(Long id) {
        Category category = getByIdOrThrow(id);
        category.deactivate();
        return category;
    }

    // slug rỗng/blank -> tự sinh từ name; slug có nội dung -> vẫn chuẩn hoá
    // qua SlugGenerator (không tin thẳng input Admin).
    private String resolveSlug(String requestedSlug, String name) {
        String source = (requestedSlug == null || requestedSlug.isBlank()) ? name : requestedSlug;
        return SlugGenerator.generate(source);
    }
}
