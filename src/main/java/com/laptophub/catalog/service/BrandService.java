package com.laptophub.catalog.service;

import com.laptophub.catalog.SlugGenerator;
import com.laptophub.catalog.dto.BrandCreateRequest;
import com.laptophub.catalog.dto.BrandUpdateRequest;
import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.BrandStatus;
import com.laptophub.catalog.repository.BrandRepository;
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
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional
    public Brand create(BrandCreateRequest request) {
        String slug = resolveSlug(request.slug(), request.name());
        if (brandRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Slug đã tồn tại");
        }
        return brandRepository.save(Brand.create(request.name(), slug, request.description(), request.logoUrl()));
    }

    @Transactional
    public Brand update(Long id, BrandUpdateRequest request) {
        Brand brand = getByIdOrThrow(id);
        String slug = resolveSlug(request.slug(), request.name());
        if (brandRepository.existsBySlugAndIdNot(slug, id)) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Slug đã tồn tại");
        }
        brand.update(request.name(), slug, request.description(), request.logoUrl());
        return brand;
    }

    public Brand getByIdOrThrow(Long id) {
        return brandRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public Page<Brand> list(Pageable pageable) {
        return brandRepository.findAll(pageable);
    }

    // Dùng bởi PublicBrandController — chỉ thương hiệu đang hiển thị.
    public List<Brand> listActive() {
        return brandRepository.findByStatusOrderByNameAsc(BrandStatus.ACTIVE);
    }

    // Batch fetch tên thương hiệu theo id — dùng bởi ProductService khi lắp
    // danh sách sản phẩm, tránh N+1.
    public Map<Long, String> findNamesByIds(List<Long> ids) {
        return brandRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Brand::getId, Brand::getName));
    }

    @Transactional
    public Brand activate(Long id) {
        Brand brand = getByIdOrThrow(id);
        brand.activate();
        return brand;
    }

    @Transactional
    public Brand deactivate(Long id) {
        Brand brand = getByIdOrThrow(id);
        brand.deactivate();
        return brand;
    }

    private String resolveSlug(String requestedSlug, String name) {
        String source = (requestedSlug == null || requestedSlug.isBlank()) ? name : requestedSlug;
        return SlugGenerator.generate(source);
    }
}
