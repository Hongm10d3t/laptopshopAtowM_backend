package com.laptophub.catalog.service;

import com.laptophub.catalog.dto.ProductVariantCreateRequest;
import com.laptophub.catalog.dto.ProductVariantUpdateRequest;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.repository.ProductVariantRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductService productService;

    public ProductVariantService(ProductVariantRepository productVariantRepository, ProductService productService) {
        this.productVariantRepository = productVariantRepository;
        this.productService = productService;
    }

    @Transactional
    public ProductVariant addVariant(Long productId, ProductVariantCreateRequest request) {
        productService.getByIdOrThrow(productId);
        if (productVariantRepository.existsBySku(request.sku())) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "SKU đã tồn tại");
        }
        ProductVariant variant = ProductVariant.create(productId, request.sku(), request.variantName(),
                request.price(), request.ramGb(), request.storageGb(), request.storageType(), request.color());
        return productVariantRepository.save(variant);
    }

    @Transactional
    public ProductVariant updateVariant(Long productId, Long variantId, ProductVariantUpdateRequest request) {
        ProductVariant variant = getOwnedOrThrow(productId, variantId);
        variant.update(request.variantName(), request.price(), request.ramGb(), request.storageGb(),
                request.storageType(), request.color());
        return variant;
    }

    // Không phân biệt "không tồn tại" và "không thuộc product này" — cả 2
    // đều RESOURCE_NOT_FOUND, giống AddressService.getOwned.
    public ProductVariant getOwnedOrThrow(Long productId, Long variantId) {
        return productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // Dùng bởi module khác (vd inventory) cần validate variant tồn tại mà
    // không có productId trong tay — khác getOwnedOrThrow vốn gắn với route
    // lồng /products/{id}/variants/{variantId}.
    public ProductVariant getByIdOrThrow(Long variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public List<ProductVariant> listByProduct(Long productId) {
        return productVariantRepository.findByProductId(productId);
    }

    @Transactional
    public ProductVariant activate(Long productId, Long variantId) {
        ProductVariant variant = getOwnedOrThrow(productId, variantId);
        variant.activate();
        return variant;
    }

    @Transactional
    public ProductVariant deactivate(Long productId, Long variantId) {
        ProductVariant variant = getOwnedOrThrow(productId, variantId);
        variant.deactivate();
        return variant;
    }
}
