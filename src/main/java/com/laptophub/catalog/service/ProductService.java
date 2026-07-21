package com.laptophub.catalog.service;

import com.laptophub.catalog.SlugGenerator;
import com.laptophub.catalog.dto.ProductCreateRequest;
import com.laptophub.catalog.dto.ProductSummaryResponse;
import com.laptophub.catalog.dto.ProductUpdateRequest;
import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.BrandStatus;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.CategoryStatus;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductStatus;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final BrandService brandService;

    public ProductService(ProductRepository productRepository, CategoryService categoryService,
                           BrandService brandService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.brandService = brandService;
    }

    @Transactional
    public Product create(ProductCreateRequest request) {
        Category category = requireActiveCategory(request.categoryId());
        Brand brand = requireActiveBrand(request.brandId());
        String slug = resolveSlug(request.slug(), request.name());
        if (productRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Slug đã tồn tại");
        }
        return productRepository.save(Product.create(category.getId(), brand.getId(), request.name(), slug,
                request.shortDescription(), request.description()));
    }

    @Transactional
    public Product update(Long id, ProductUpdateRequest request) {
        Product product = getByIdOrThrow(id);
        Category category = requireActiveCategory(request.categoryId());
        Brand brand = requireActiveBrand(request.brandId());
        String slug = resolveSlug(request.slug(), request.name());
        if (productRepository.existsBySlugAndIdNot(slug, id)) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Slug đã tồn tại");
        }
        product.update(request.name(), slug, request.shortDescription(), request.description());
        product.changeCategory(category.getId());
        product.changeBrand(brand.getId());
        return product;
    }

    public Product getByIdOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // Danh sách cần tên category/brand cho từng dòng — enrich bằng batch
    // fetch (findNamesByIds) ngay tại đây để tránh N+1, khác với các
    // mutation phía trên (chỉ 1 sản phẩm/lần nên controller tự tra tên rời
    // qua CategoryService/BrandService.getByIdOrThrow là đủ, không cần batch).
    public Page<ProductSummaryResponse> listAdmin(Long categoryId, Long brandId, ProductStatus status,
                                                   String keyword, Pageable pageable) {
        Page<Product> page = productRepository.searchAdmin(categoryId, brandId, status, keyword, pageable);
        Map<Long, String> categoryNames = categoryService.findNamesByIds(
                page.getContent().stream().map(Product::getCategoryId).distinct().toList());
        Map<Long, String> brandNames = brandService.findNamesByIds(
                page.getContent().stream().map(Product::getBrandId).distinct().toList());
        return page.map(p -> ProductSummaryResponse.from(p, categoryNames.get(p.getCategoryId()),
                brandNames.get(p.getBrandId())));
    }

    @Transactional
    public Product activate(Long id) {
        Product product = getByIdOrThrow(id);
        product.activate();
        return product;
    }

    @Transactional
    public Product deactivate(Long id) {
        Product product = getByIdOrThrow(id);
        product.deactivate();
        return product;
    }

    private Category requireActiveCategory(Long categoryId) {
        Category category = categoryService.getByIdOrThrow(categoryId);
        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Danh mục đã ngừng hoạt động");
        }
        return category;
    }

    private Brand requireActiveBrand(Long brandId) {
        Brand brand = brandService.getByIdOrThrow(brandId);
        if (brand.getStatus() != BrandStatus.ACTIVE) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Thương hiệu đã ngừng hoạt động");
        }
        return brand;
    }

    private String resolveSlug(String requestedSlug, String name) {
        String source = (requestedSlug == null || requestedSlug.isBlank()) ? name : requestedSlug;
        return SlugGenerator.generate(source);
    }
}
