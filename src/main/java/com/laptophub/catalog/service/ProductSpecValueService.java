package com.laptophub.catalog.service;

import com.laptophub.catalog.dto.ProductSpecValueResponse;
import com.laptophub.catalog.dto.ProductSpecValuesUpsertRequest;
import com.laptophub.catalog.entity.ProductSpecValue;
import com.laptophub.catalog.entity.SpecificationDefinition;
import com.laptophub.catalog.repository.ProductSpecValueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductSpecValueService {

    private final ProductSpecValueRepository productSpecValueRepository;
    private final ProductService productService;
    private final SpecificationDefinitionService specificationDefinitionService;

    public ProductSpecValueService(ProductSpecValueRepository productSpecValueRepository,
                                    ProductService productService,
                                    SpecificationDefinitionService specificationDefinitionService) {
        this.productSpecValueRepository = productSpecValueRepository;
        this.productService = productService;
        this.specificationDefinitionService = specificationDefinitionService;
    }

    // Thay toàn bộ set giá trị của product bằng danh sách trong request: xóa
    // giá trị không còn nằm trong danh sách mới, cập nhật giá trị đã có,
    // thêm giá trị chưa có — không có "xóa từng phần" riêng, request luôn
    // đại diện cho toàn bộ mong muốn (đúng chú thích ProductSpecValuesUpsertRequest).
    @Transactional
    public List<ProductSpecValueResponse> upsertValues(Long productId, ProductSpecValuesUpsertRequest request) {
        productService.getByIdOrThrow(productId);

        List<Long> specDefinitionIds = request.values().stream()
                .map(ProductSpecValuesUpsertRequest.SpecValueItem::specificationDefinitionId)
                .toList();
        Map<Long, SpecificationDefinition> definitionsById = specificationDefinitionService.findByIds(specDefinitionIds);
        for (Long specDefinitionId : specDefinitionIds) {
            if (!definitionsById.containsKey(specDefinitionId)) {
                specificationDefinitionService.getByIdOrThrow(specDefinitionId);
            }
        }

        if (specDefinitionIds.isEmpty()) {
            productSpecValueRepository.deleteAll(productSpecValueRepository.findByProductId(productId));
        } else {
            productSpecValueRepository.deleteByProductIdAndSpecificationDefinitionIdNotIn(productId, specDefinitionIds);
        }

        Map<Long, ProductSpecValue> existingByDefinitionId = new HashMap<>();
        for (ProductSpecValue existing : productSpecValueRepository.findByProductId(productId)) {
            existingByDefinitionId.put(existing.getSpecificationDefinitionId(), existing);
        }

        List<ProductSpecValueResponse> responses = new ArrayList<>();
        for (ProductSpecValuesUpsertRequest.SpecValueItem item : request.values()) {
            ProductSpecValue value = existingByDefinitionId.get(item.specificationDefinitionId());
            if (value != null) {
                value.changeValue(item.value());
            } else {
                value = productSpecValueRepository.save(
                        ProductSpecValue.create(productId, item.specificationDefinitionId(), item.value()));
            }
            responses.add(ProductSpecValueResponse.from(value, definitionsById.get(item.specificationDefinitionId())));
        }
        return responses;
    }

    public List<ProductSpecValueResponse> listByProduct(Long productId) {
        List<ProductSpecValue> values = productSpecValueRepository.findByProductId(productId);
        Map<Long, SpecificationDefinition> definitionsById = specificationDefinitionService.findByIds(
                values.stream().map(ProductSpecValue::getSpecificationDefinitionId).distinct().toList());
        return values.stream()
                .map(value -> ProductSpecValueResponse.from(value, definitionsById.get(value.getSpecificationDefinitionId())))
                .toList();
    }
}
