package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.SpecificationDefinition;
import com.laptophub.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Migration V12 đã seed sẵn ~9 spec baseline (category_id = NULL) — test ở
// đây phải tính đến dữ liệu seed đó thay vì giả định bảng rỗng.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class SpecificationDefinitionRepositoryTest {

    @Autowired
    private SpecificationDefinitionRepository specificationDefinitionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void seedData_fromMigrationV12_isPresent() {
        List<SpecificationDefinition> all = specificationDefinitionRepository.findAllByOrderByDisplayOrderAsc();

        assertThat(all).hasSizeGreaterThanOrEqualTo(9);
        assertThat(all).extracting(SpecificationDefinition::getCode).contains("cpu", "gpu", "ram_type");
    }

    @Test
    void findApplicableToCategory_includesGlobalAndCategorySpecificSpecs() {
        Long categoryId = categoryRepository.saveAndFlush(Category.create("Cat spec-test", "cat-spec-test", null))
                .getId();
        Long otherCategoryId = categoryRepository.saveAndFlush(
                Category.create("Cat spec-test-other", "cat-spec-test-other", null)).getId();
        SpecificationDefinition categorySpecific = specificationDefinitionRepository.saveAndFlush(
                SpecificationDefinition.create(categoryId, "hinge_type_spec_test", "Loai ban le", null, null, 100));

        List<SpecificationDefinition> forCategory =
                specificationDefinitionRepository.findApplicableToCategory(categoryId);
        List<SpecificationDefinition> forOtherCategory =
                specificationDefinitionRepository.findApplicableToCategory(otherCategoryId);

        assertThat(forCategory).extracting(SpecificationDefinition::getId).contains(categorySpecific.getId());
        assertThat(forCategory).extracting(SpecificationDefinition::getCode).contains("cpu");
        assertThat(forOtherCategory).extracting(SpecificationDefinition::getId).doesNotContain(categorySpecific.getId());
        assertThat(forOtherCategory).extracting(SpecificationDefinition::getCode).contains("cpu");
    }
}
