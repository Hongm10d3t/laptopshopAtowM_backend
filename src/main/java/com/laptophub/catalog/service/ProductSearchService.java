package com.laptophub.catalog.service;

import com.laptophub.catalog.dto.ProductDetailResponse;
import com.laptophub.catalog.dto.ProductImageResponse;
import com.laptophub.catalog.dto.ProductListItemResponse;
import com.laptophub.catalog.dto.ProductSortOption;
import com.laptophub.catalog.dto.ProductVariantResponse;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductImage;
import com.laptophub.catalog.entity.ProductVariantStatus;
import com.laptophub.catalog.repository.ProductImageRepository;
import com.laptophub.catalog.repository.ProductPriceRange;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.catalog.repository.ProductVariantRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Read-only, dành cho Guest/Customer (route /public/**). Không dùng
// Pageable/Sort của Spring Data cho phần sort — xem lý do ở
// ProductRepository.searchPublic và ProductSortOption.
@Service
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final ProductSpecValueService productSpecValueService;

    public ProductSearchService(ProductRepository productRepository, ProductVariantRepository productVariantRepository,
                                 ProductImageRepository productImageRepository, CategoryService categoryService,
                                 BrandService brandService, ProductSpecValueService productSpecValueService) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.productSpecValueService = productSpecValueService;
    }

    public Page<ProductListItemResponse> search(Long categoryId, Long brandId, String keyword, BigDecimal minPrice,
                                                 BigDecimal maxPrice, ProductSortOption sort, Pageable pageable) {
        List<Product> matched = productRepository.searchPublic(categoryId, brandId, keyword, minPrice, maxPrice);
        int total = matched.size();
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Map<Long, ProductPriceRange> priceRangesByProductId = productVariantRepository
                .findMinMaxPriceByProductIdIn(matched.stream().map(Product::getId).toList()).stream()
                .collect(Collectors.toMap(ProductPriceRange::getProductId, r -> r));

        List<Product> sorted = matched.stream()
                .sorted(resolveComparator(sort, priceRangesByProductId))
                .toList();

        int fromIndex = Math.min((int) pageable.getOffset(), total);
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        List<Product> pageContent = sorted.subList(fromIndex, toIndex);

        Map<Long, String> categoryNames = categoryService.findNamesByIds(
                pageContent.stream().map(Product::getCategoryId).distinct().toList());
        Map<Long, String> brandNames = brandService.findNamesByIds(
                pageContent.stream().map(Product::getBrandId).distinct().toList());
        Map<Long, ProductImage> thumbnailsByProductId = productImageRepository
                .findByProductIdInOrderBySortOrderAsc(pageContent.stream().map(Product::getId).toList()).stream()
                .collect(Collectors.toMap(ProductImage::getProductId, i -> i, (first, second) -> first));

        List<ProductListItemResponse> content = pageContent.stream()
                .map(p -> {
                    ProductPriceRange range = priceRangesByProductId.get(p.getId());
                    ProductImage thumbnail = thumbnailsByProductId.get(p.getId());
                    return new ProductListItemResponse(
                            p.getId(), p.getName(), p.getSlug(),
                            categoryNames.get(p.getCategoryId()), brandNames.get(p.getBrandId()),
                            range.getMinPrice(), range.getMaxPrice(),
                            thumbnail != null ? thumbnail.getUrl() : null);
                })
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    public ProductDetailResponse getDetailBySlug(String slug) {
        Product product = productRepository.findPublicBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        String categoryName = categoryService.getByIdOrThrow(product.getCategoryId()).getName();
        String brandName = brandService.getByIdOrThrow(product.getBrandId()).getName();
        List<ProductVariantResponse> variants = productVariantRepository
                .findByProductIdAndStatus(product.getId(), ProductVariantStatus.ACTIVE).stream()
                .map(ProductVariantResponse::from)
                .toList();
        List<ProductImageResponse> images = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId())
                .stream()
                .map(ProductImageResponse::from)
                .toList();

        return new ProductDetailResponse(
                product.getId(), product.getName(), product.getSlug(), categoryName, brandName,
                product.getShortDescription(), product.getDescription(), images, variants,
                productSpecValueService.listByProduct(product.getId()));
    }

    private Comparator<Product> resolveComparator(ProductSortOption sort, Map<Long, ProductPriceRange> priceRanges) {
        return switch (sort) {
            case NAME_ASC -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            case NAME_DESC -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER).reversed();
            case PRICE_ASC -> Comparator.comparing(p -> priceRanges.get(p.getId()).getMinPrice());
            case PRICE_DESC -> Comparator.<Product, BigDecimal>comparing(p -> priceRanges.get(p.getId()).getMinPrice())
                    .reversed();
            case NEWEST -> Comparator.comparing(Product::getCreatedAt).reversed();
        };
    }
}
