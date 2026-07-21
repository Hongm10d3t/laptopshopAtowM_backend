package com.laptophub.catalog.service;

import com.laptophub.catalog.entity.SpecificationDefinition;
import com.laptophub.catalog.repository.SpecificationDefinitionRepository;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Chỉ đọc — dữ liệu tham chiếu ít thay đổi, seed sẵn qua migration
// (V10-V12), chưa có Admin CRUD ở giai đoạn này.
@Service
public class SpecificationDefinitionService {

    private final SpecificationDefinitionRepository specificationDefinitionRepository;

    public SpecificationDefinitionService(SpecificationDefinitionRepository specificationDefinitionRepository) {
        this.specificationDefinitionRepository = specificationDefinitionRepository;
    }

    public List<SpecificationDefinition> listAll() {
        return specificationDefinitionRepository.findAllByOrderByDisplayOrderAsc();
    }

    // categoryId null -> trả toàn bộ (không lọc theo danh mục cụ thể).
    public List<SpecificationDefinition> listForCategory(Long categoryId) {
        if (categoryId == null) {
            return listAll();
        }
        return specificationDefinitionRepository.findApplicableToCategory(categoryId);
    }

    public SpecificationDefinition getByIdOrThrow(Long id) {
        return specificationDefinitionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // Batch fetch theo id — dùng bởi ProductSpecValueService khi lắp response
    // cho danh sách giá trị của 1 sản phẩm, tránh N+1.
    public Map<Long, SpecificationDefinition> findByIds(List<Long> ids) {
        return specificationDefinitionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(SpecificationDefinition::getId, s -> s));
    }
}
