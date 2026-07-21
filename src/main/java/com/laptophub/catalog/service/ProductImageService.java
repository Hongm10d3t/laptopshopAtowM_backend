package com.laptophub.catalog.service;

import com.laptophub.catalog.dto.ProductImageCreateRequest;
import com.laptophub.catalog.dto.ProductImageReorderRequest;
import com.laptophub.catalog.entity.ProductImage;
import com.laptophub.catalog.repository.ProductImageRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductService productService;

    public ProductImageService(ProductImageRepository productImageRepository, ProductService productService) {
        this.productImageRepository = productImageRepository;
        this.productService = productService;
    }

    // sortOrder không truyền -> nối vào cuối danh sách hiện có.
    @Transactional
    public ProductImage addImage(Long productId, ProductImageCreateRequest request) {
        productService.getByIdOrThrow(productId);
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : nextSortOrder(productId);
        return productImageRepository.save(ProductImage.create(productId, request.url(), request.altText(), sortOrder));
    }

    @Transactional
    public void removeImage(Long productId, Long imageId) {
        ProductImage image = getOwnedOrThrow(productId, imageId);
        productImageRepository.delete(image);
    }

    // Vị trí trong request.orderedImageIds() (index) trở thành sort_order
    // mới của từng ảnh — ảnh không có trong danh sách giữ nguyên sort_order cũ.
    @Transactional
    public List<ProductImage> reorderImages(Long productId, ProductImageReorderRequest request) {
        List<ProductImage> reordered = new ArrayList<>();
        List<Long> orderedIds = request.orderedImageIds();
        for (int index = 0; index < orderedIds.size(); index++) {
            ProductImage image = getOwnedOrThrow(productId, orderedIds.get(index));
            image.changeSortOrder(index);
            reordered.add(image);
        }
        return reordered;
    }

    public List<ProductImage> listByProduct(Long productId) {
        return productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
    }

    public ProductImage getOwnedOrThrow(Long productId, Long imageId) {
        return productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private int nextSortOrder(Long productId) {
        List<ProductImage> existing = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
        return existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getSortOrder() + 1;
    }
}
